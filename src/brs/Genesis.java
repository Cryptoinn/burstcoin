package brs;

public final class Genesis {

  public static final long GENESIS_BLOCK_ID = 1109294670862540038L;
  public static final long CREATOR_ID = 0L;
  public static final byte[] CREATOR_PUBLIC_KEY = {
    1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0,
    0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1,
  };

  public static final long[] GENESIS_RECIPIENTS = {};

  public static final int[] GENESIS_AMOUNTS = {};

  public static final byte[][] GENESIS_SIGNATURES = {};

  public static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
    1, 2, 5, 7, 3, 1, 6, 2, 0, 6, 1, 0, 2, 0, 0, 9,
    1, 1, 0, 9, 9, 8, 5, 3, 2, 7, 9, 1, 0, 0, 2, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };

  private Genesis() {} // never

}
