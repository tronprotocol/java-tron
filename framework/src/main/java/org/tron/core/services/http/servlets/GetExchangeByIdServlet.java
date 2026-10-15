package org.tron.core.services.http.servlets;

import com.google.protobuf.ByteString;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.json.JSONObject;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getexchangebyid", access = Access.READ,
    surfaces = {Surface.FULL, Surface.SOLIDITY, Surface.PBFT, Surface.SOLIDITY_NODE})
public class GetExchangeByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      long id = Util.getJsonLongValue(jsonObject, "id", true);
      fillResponse(params.isVisible(), id, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("id");
      fillResponse(visible, Long.parseLong(input), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, long id, HttpServletResponse response)
      throws IOException {
    response.getWriter().println(JsonFormat.printToString(
        wallet.getExchangeById(ByteString.copyFrom(ByteArray.fromLong(id))), visible));
  }
}