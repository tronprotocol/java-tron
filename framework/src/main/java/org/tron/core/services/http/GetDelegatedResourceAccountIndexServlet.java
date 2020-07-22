package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;


@Component
@Slf4j(topic = "API")
public class GetDelegatedResourceAccountIndexServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String address = request.getParameter("value");
      if (visible) {
        address = Util.getHexAddress(address);
      }
      fillResponse(ByteString.copyFrom(ByteArray.fromHexString(address)), visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      boolean visible = params.isVisible();
      String input = params.getParams();
      if (visible) {
        JSONObject jsonObject = JSONObject.parseObject(input);
        String value = jsonObject.getString("value");
        jsonObject.put("value", Util.getHexAddress(value));
        input = jsonObject.toJSONString();
      }

      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);

      fillResponse(build.getValue(), visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(ByteString address, boolean visible, HttpServletResponse response)
      throws IOException {
    DelegatedResourceAccountIndex reply =
        wallet.getDelegatedResourceAccountIndex(address);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
