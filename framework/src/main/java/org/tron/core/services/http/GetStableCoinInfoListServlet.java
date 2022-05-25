package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.protos.contract.StableMarketContractOuterClass;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Slf4j(topic = "API")
public class GetStableCoinInfoListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      fillResponse(visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      fillResponse(params.isVisible(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, HttpServletResponse response)
      throws IOException {
    StableMarketContractOuterClass.StableCoinInfoList stableCoinInfoList = wallet.getStableCoinList();
    if (stableCoinInfoList != null) {
      response.getWriter().println(JsonFormat.printToString(stableCoinInfoList, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}