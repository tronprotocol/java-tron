package org.tron.core.services.http;

import static org.tron.core.services.http.PostParams.S_VALUE;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;


@Component
@Slf4j(topic = "API")
public class CreateAddressServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter(S_VALUE);
      if (visible) {
        input = Util.getHexString(input);
      }
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(S_VALUE, input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);
      fillResponse(build.getValue(), response);
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
        input = covertStringToHex(input);
      }
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      fillResponse(build.getValue(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private String covertStringToHex(String input) {
    JSONObject jsonObject = JSONObject.parseObject(input);
    String value = jsonObject.getString(S_VALUE);
    jsonObject.put(S_VALUE, Util.getHexString(value));
    return jsonObject.toJSONString();
  }

  private void fillResponse(ByteString value, HttpServletResponse response) throws IOException {
    byte[] address = wallet.createAddress(value.toByteArray());
    String base58check = StringUtil.encode58Check(address);
    String hexString = ByteArray.toHexString(address);
    JSONObject jsonAddress = new JSONObject();
    jsonAddress.put("base58checkAddress", base58check);
    jsonAddress.put(S_VALUE, hexString);
    response.getWriter().println(jsonAddress.toJSONString());
  }
}
