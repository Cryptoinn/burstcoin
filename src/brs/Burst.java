package brs;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.github.gquintana.metrics.util.SqlObjectNameFactory;
import brs.db.firebird.FirebirdDbs;
import brs.db.firebird.FirebirdStores;
import brs.db.h2.H2Dbs;
import brs.db.h2.H2Stores;
import brs.db.mariadb.MariadbDbs;
import brs.db.mariadb.MariadbStores;
import brs.db.sql.Db;
import brs.db.store.Dbs;
import brs.db.store.Stores;
import brs.http.API;
import brs.peer.Peers;
import brs.user.Users;
import brs.util.LoggerConfigurator;
import brs.util.ThreadPool;
import brs.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Burst {

  public static final String VERSION = "0.1 Test";
  public static final String APPLICATION = "BRS";
  public static final MetricRegistry metrics = new MetricRegistry();
  private static final Logger logger = LoggerFactory.getLogger(Burst.class);
  private static final Properties defaultProperties = new Properties();
  private static final Properties properties = new Properties(defaultProperties);
  private static volatile Time time = new Time.EpochTime();
  private static Stores stores;
  private static Dbs dbs;
  private static Generator generator = new GeneratorImpl();

  static {
    System.out.println("Initializing Burst server version " + Burst.VERSION);
    try (InputStream is = ClassLoader.getSystemResourceAsStream("brs-default.properties")) {
      if (is != null) {
        Burst.defaultProperties.load(is);
      }
      else {
        String configFile = System.getProperty("brs-default.properties");
        if (configFile != null) {
          try (InputStream fis = new FileInputStream(configFile)) {
            Burst.defaultProperties.load(fis);
          } catch (IOException e) {
            throw new RuntimeException("Error loading brs-default.properties from " + configFile);
          }
        } else {
          throw new RuntimeException("brs-default.properties not in classpath and system property brs-default.properties not defined either");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading brs-default.properties", e);
    }
  }

  static {
    try (InputStream is = ClassLoader.getSystemResourceAsStream("brs.properties")) {
      if (is != null) { // parse if brs.properties was loaded
        Burst.properties.load(is);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Error loading brs.properties", e);
    }
  }

  private Burst() {
  } // never

  public static int getIntProperty(String name) {
    try {
      int result = Integer.parseInt(properties.getProperty(name));
      logger.info(name + " = \"" + result + "\"");
      return result;
    }
    catch (NumberFormatException e) {
      logger.info(name + " not defined, assuming 0");
      return 0;
    }
  }
  
  public static int getIntProperty(String name, int defaultValue) {
    try {
      int result = Integer.parseInt(properties.getProperty(name));
      logger.info(name + " = \"" + result + "\"");
      return result;
    } catch (NumberFormatException e) {
      logger.info(name + " not defined, assuming "+defaultValue);
      return defaultValue;
    }
  }

  public static String getStringProperty(String name) {
    return getStringProperty(name, null);
  }

  public static String getStringProperty(String name, String defaultValue) {
    String value = properties.getProperty(name);
    if (value != null && !"".equals(value)) {
      logger.info(name + " = \"" + value + "\"");
      return value;
    }

    logger.info(name + " not defined");
    return defaultValue;
  }

  public static List<String> getStringListProperty(String name) {
    String value = getStringProperty(name);
    if (value == null || value.length() == 0) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (String s : value.split(";")) {
      s = s.trim();
      if (s.length() > 0) {
        result.add(s);
      }
    }
    return result;
  }

  public static Boolean getBooleanProperty(String name) {
    String value = properties.getProperty(name);

    if (Boolean.TRUE.toString().equals(value)) {
      logger.info(name + " = \"true\"");
      return true;
    }

    if (Boolean.FALSE.toString().equals(value)) {
      logger.info(name + " = \"false\"");
      return false;
    }

    logger.info(name + " not defined, assuming false");
    return false;
  }

  public static Boolean getBooleanProperty(String name, boolean assume) {
    String value = properties.getProperty(name);
    if (Boolean.TRUE.toString().equals(value)) {
      logger.info(name + " = \"true\"");
      return true;
    }
    if (Boolean.FALSE.toString().equals(value)) {
      logger.info(name + " = \"false\"");
      return false;
    }
    logger.info(name + " not defined, assuming " + assume);
    return assume;
  }

  public static Blockchain getBlockchain() {
    return BlockchainImpl.getInstance();
  }

  public static BlockchainProcessor getBlockchainProcessor() {
    return BlockchainProcessorImpl.getInstance();
  }

  public static TransactionProcessor getTransactionProcessor() {
    return TransactionProcessorImpl.getInstance();
  }

  public static Generator getGenerator() {
    return generator;
  }

  public static void setGenerator(Generator newGenerator) {
    generator = newGenerator;
  }

  public static int getEpochTime() {
    return time.getTime();
  }

  static void setTime(Time time) {
    Burst.time = time;
  }

  public static void main(String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        @Override
        public void run() {
          Burst.shutdown();
        }
      }));
    init();
  }

  public static void init(Properties customProperties) {
    properties.putAll(customProperties);
    init();
  }

  public static void init() {
    Init.init();
  }

  public static void shutdown() {
    logger.info("Shutting down...");
    API.shutdown();
    Users.shutdown();
    Peers.shutdown();
    ThreadPool.shutdown();
    Db.shutdown();
    if (BlockchainProcessorImpl.oclVerify) {
      OCLPoC.destroy();
    }
    logger.info("Burst server " + VERSION + " stopped.");
    LoggerConfigurator.shutdown();
  }

  public static Stores getStores() {
    return stores;
  }

  public static Dbs getDbs() {
    return dbs;
  }

  private static class Init {

    static {
      try {
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).createsObjectNamesWith(new SqlObjectNameFactory()).build();
        reporter.start();

        long startTime = System.currentTimeMillis();

        LoggerConfigurator.init();

        String dbUrl;
        if (Constants.isTestnet) {
          dbUrl = Burst.getStringProperty("brs.testDbUrl");
        }
        else {
          dbUrl = Burst.getStringProperty("brs.dbUrl");
        }

        Db.init();
        switch (Db.getDatabaseType()) {
          case MARIADB:
            logger.info("Using mariadb Backend");
            dbs = new MariadbDbs();
            stores = new MariadbStores();
            break;
          case FIREBIRD:
            logger.info("Using Firebird Backend");
            dbs = new FirebirdDbs();
            stores = new FirebirdStores();
            break;
          case H2:
            logger.info("Using h2 Backend");
            dbs = new H2Dbs();
            stores = new H2Stores();
            break;
          default:
            throw new RuntimeException("Error initializing wallet: Unknown database type");
        }
        TransactionProcessorImpl.getInstance();
        BlockchainProcessorImpl.getInstance();


        Account.init();
        Alias.init();
        Asset.init();
        DigitalGoodsStore.init();
        Hub.init();
        Order.init();
        Poll.init();
        Trade.init();
        AssetTransfer.init();
        Vote.init();
        AT.init();
        Peers.init();
        getGenerator().init();
        API.init();
        Users.init();
        DebugTrace.init();

        int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Burst.getIntProperty("brs.timeMultiplier"), 1) : 1;

        ThreadPool.start(timeMultiplier);
        if (timeMultiplier > 1) {
          setTime(new Time.FasterTime(Math.max(getEpochTime(), Burst.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
          logger.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
        }

        long currentTime = System.currentTimeMillis();
        logger.info("Initialization took " + (currentTime - startTime) + " ms");
        logger.info("Burst server " + VERSION + " started successfully.");

        if (Constants.isTestnet) {
          logger.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
        }
        if (Burst.getBooleanProperty("brs.mockMining")) {
          setGenerator(new GeneratorImpl.MockGeneratorImpl());
        }

        if (BlockchainProcessorImpl.oclVerify) {
          try {
            OCLPoC.init();
          }
          catch (OCLPoC.OCLCheckerException e) {
            logger.error("Error initializing OpenCL, disabling ocl verify: " + e.getMessage());
            BlockchainProcessorImpl.oclVerify = false;
          }
        }
      }
      catch (Exception e) {
        logger.error(e.getMessage(), e);
        System.exit(1);
      }
    }

    //    private Init() {
    //  logger.info("private Init");
    // } // never

    private static void init() {
    }
  }
}
