package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.MarketOrderList;


@Component
@Slf4j(topic = "API")
public class GetMarketOrderByAccountServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void getResult(String address, boolean visible, HttpServletResponse response)
      throws IOException {
    if (visible) {
      address = Util.getHexAddress(address);
    }

    MarketOrderList reply = wallet
        .getMarketOrderByAccount(ByteString.copyFrom(ByteArray.fromHexString(address)));
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String address = request.getParameter("value");

      getResult(address, visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSON.parseObject(params.getParams());
      String value = jsonObject.getString("value");

      getResult(value, params.isVisible(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
