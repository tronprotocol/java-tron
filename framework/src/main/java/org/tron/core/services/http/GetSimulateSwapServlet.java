package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractExeException;
import org.tron.protos.contract.StableMarketContract;

@Component
@Slf4j(topic = "API")
public class GetSimulateSwapServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String sourceAsset = request.getParameter("source_asset");
      String destAsset = request.getParameter("dest_asset");
      long amount = Long.getLong(request.getParameter("amount"));
      fillResponse(sourceAsset.getBytes(), destAsset.getBytes(), amount, visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      String sourceAsset = jsonObject.getString("source_asset");
      String destAsset = jsonObject.getString("dest_asset");
      long amount = jsonObject.getLongValue("amount");
      fillResponse(sourceAsset.getBytes(), destAsset.getBytes(),
          amount, params.isVisible(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(byte[] sourceAsset, byte[] destAsset,
                            long amount, boolean visible, HttpServletResponse response)
      throws IOException, ContractExeException {
    StableMarketContract.ExchangeResult exchangeResult =
        wallet.getSimulateSwap(sourceAsset, destAsset, amount);
    if (exchangeResult != null) {
      response.getWriter().println(JsonFormat.printToString(exchangeResult, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}