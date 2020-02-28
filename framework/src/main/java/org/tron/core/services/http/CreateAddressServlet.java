package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
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
      String input = request.getParameter("value");
      if (visible) {
        input = Util.getHexString(input);
      }
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("value", input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);
      byte[] address = wallet.createAddress(build.getValue().toByteArray());
      String base58check = StringUtil.encode58Check(address);
      String hexString = ByteArray.toHexString(address);
      JSONObject jsonAddress = new JSONObject();
      jsonAddress.put("base58checkAddress", base58check);
      jsonAddress.put("value", hexString);
      response.getWriter().println(jsonAddress.toJSONString());
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private String covertStringToHex(String input) {
    JSONObject jsonObject = JSONObject.parseObject(input);
    String value = jsonObject.getString("value");
    jsonObject.put("value", Util.getHexString(value));
    return jsonObject.toJSONString();
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      if (visible) {
        input = covertStringToHex(input);
      }
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      byte[] address = wallet.createAddress(build.getValue().toByteArray());
      String base58check = StringUtil.encode58Check(address);
      String hexString = ByteArray.toHexString(address);
      JSONObject jsonAddress = new JSONObject();
      jsonAddress.put("base58checkAddress", base58check);
      jsonAddress.put("value", hexString);
      response.getWriter().println(jsonAddress.toJSONString());
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
