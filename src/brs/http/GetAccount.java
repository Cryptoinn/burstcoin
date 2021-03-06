package brs.http;

import brs.Account;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccount extends APIServlet.APIRequestHandler {

  static final GetAccount instance = new GetAccount();

  private GetAccount() {
    super(new APITag[] {APITag.ACCOUNTS}, "account");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = ParameterParser.getAccount(req);

    JSONObject response = JSONData.accountBalance(account);
    JSONData.putAccount(response, "account", account.getId());

    if (account.getPublicKey() != null) {
      response.put("publicKey", Convert.toHexString(account.getPublicKey()));
    }
    if (account.getName() != null) {
      response.put("name", account.getName());
    }
    if (account.getDescription() != null) {
      response.put("description", account.getDescription());
    }


    try (BurstIterator<Account.AccountAsset> accountAssets = account.getAssets(0, -1)) {
      JSONArray assetBalances = new JSONArray();
      JSONArray unconfirmedAssetBalances = new JSONArray();
      while (accountAssets.hasNext()) {
        Account.AccountAsset accountAsset = accountAssets.next();
        JSONObject assetBalance = new JSONObject();
        assetBalance.put("asset", Convert.toUnsignedLong(accountAsset.getAssetId()));
        assetBalance.put("balanceQNT", String.valueOf(accountAsset.getQuantityQNT()));
        assetBalances.add(assetBalance);
        JSONObject unconfirmedAssetBalance = new JSONObject();
        unconfirmedAssetBalance.put("asset", Convert.toUnsignedLong(accountAsset.getAssetId()));
        unconfirmedAssetBalance.put("unconfirmedBalanceQNT", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
        unconfirmedAssetBalances.add(unconfirmedAssetBalance);
      }
      if (assetBalances.size() > 0) {
        response.put("assetBalances", assetBalances);
      }
      if (unconfirmedAssetBalances.size() > 0) {
        response.put("unconfirmedAssetBalances", unconfirmedAssetBalances);
      }
    }
    return response;

  }

}
