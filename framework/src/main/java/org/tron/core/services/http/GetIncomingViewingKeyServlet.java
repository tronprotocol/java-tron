package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetIncomingViewingKeyServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      String ak = jsonObject.getString("ak");
      String nk = jsonObject.getString("nk");

      fillResponse(params.isVisible(), ak, nk, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String ak = request.getParameter("ak");
      String nk = request.getParameter("nk");

      fillResponse(visible, ak, nk, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, String ak, String nk, HttpServletResponse response)
      throws Exception {

    GrpcAPI.IncomingViewingKeyMessage ivk = wallet
        .getIncomingViewingKey(ByteArray.fromHexString(ak), ByteArray.fromHexString(nk));

    response.getWriter()
        .println(JsonFormat.printToString(ivk, visible));
  }
}
