package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Longs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractExeException;
import org.tron.protos.contract.StableMarketContractOuterClass;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Slf4j(topic = "API")
public class GetSimulateSwapServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String sourceToken = request.getParameter("source_token");
      String destToken = request.getParameter("dest_token");
      long amount = Long.getLong(request.getParameter("amount"));
      fillResponse(sourceToken.getBytes(), destToken.getBytes(), amount, visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      String sourceToken = jsonObject.getString("source_token");
      String destToken = jsonObject.getString("dest_token");
      long amount = jsonObject.getLongValue("amount");
      fillResponse(sourceToken.getBytes(), destToken.getBytes(), amount, params.isVisible(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(byte[] sourceToken, byte[] destToken, long amount, boolean visible, HttpServletResponse response)
      throws IOException, ContractExeException {
    StableMarketContractOuterClass.ExchangeResult exchangeResult = wallet.getSimulateSwap(sourceToken, destToken, amount);
    if (exchangeResult != null) {
      response.getWriter().println(JsonFormat.printToString(exchangeResult, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}