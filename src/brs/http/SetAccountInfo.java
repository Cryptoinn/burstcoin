package brs.http;

import brs.Account;
import brs.Attachment;
import brs.Constants;
import brs.BurstException;
import brs.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT_NAME_LENGTH;

public final class SetAccountInfo extends CreateTransaction {

  static final SetAccountInfo instance = new SetAccountInfo();

  private SetAccountInfo() {
    super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "name", "description");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String name = Convert.nullToEmpty(req.getParameter("name")).trim();
    String description = Convert.nullToEmpty(req.getParameter("description")).trim();

    if (name.length() > Constants.MAX_ACCOUNT_NAME_LENGTH) {
      return INCORRECT_ACCOUNT_NAME_LENGTH;
    }

    if (description.length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
      return INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
    }

    Account account = ParameterParser.getSenderAccount(req);
    Attachment attachment = new Attachment.MessagingAccountInfo(name, description);
    return createTransaction(req, account, attachment);

  }

}
