package brs.http;

import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGoods extends APIServlet.APIRequestHandler {

  static final GetDGSGoods instance = new GetDGSGoods();

  private GetDGSGoods() {
    super(new APITag[] {APITag.DGS}, "seller", "firstIndex", "lastIndex", "inStockOnly");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long sellerId = ParameterParser.getSellerId(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

    JSONObject response = new JSONObject();
    JSONArray goodsJSON = new JSONArray();
    response.put("goods", goodsJSON);

    BurstIterator<DigitalGoodsStore.Goods> goods = null;
    try {
      if (sellerId == 0) {
        if (inStockOnly) {
          goods = DigitalGoodsStore.getGoodsInStock(firstIndex, lastIndex);
        } else {
          goods = DigitalGoodsStore.getAllGoods(firstIndex, lastIndex);
        }
      } else {
        goods = DigitalGoodsStore.getSellerGoods(sellerId, inStockOnly, firstIndex, lastIndex);
      }
      while (goods.hasNext()) {
        DigitalGoodsStore.Goods good = goods.next();
        goodsJSON.add(JSONData.goods(good));
      }
    } finally {
      DbUtils.close(goods);
    }

    return response;
  }

}
