package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BlockReq;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;


@Component
@Slf4j(topic = "API")
public class GetBlockServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    handle(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    handle(request, response);
  }

  private void handle(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = parseParams(request);
      BlockReq message = buildRequest(params.getParams(), params.isVisible());
      fillResponse(params.isVisible(), message, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private PostParams parseParams(HttpServletRequest request) throws Exception {
    HttpMethod m = HttpMethod.fromString(request.getMethod());
    if (HttpMethod.GET.equals(m)) {
      String idOrNum = request.getParameter("id_or_num");
      JSONObject params = new JSONObject();
      if (!Strings.isNullOrEmpty(idOrNum)) {
        params.put("id_or_num", idOrNum);
      }
      params.put("detail", Boolean.parseBoolean(request.getParameter("detail")));
      return new PostParams(JSON.toJSONString(params),
          Boolean.parseBoolean(request.getParameter(Util.VISIBLE)));
    }
    if (HttpMethod.POST.equals(m)) {
      return PostParams.getPostParams(request);
    }
    throw new UnsupportedOperationException();
  }

  private BlockReq buildRequest(String params, boolean visible)
      throws JsonFormat.ParseException {
    BlockReq.Builder build = BlockReq.newBuilder();
    if (!Strings.isNullOrEmpty(params)) {
      JsonFormat.merge(params, build, visible);
    }
    return build.build();
  }

  private void fillResponse(boolean visible, BlockReq request, HttpServletResponse response)
      throws IOException {
    try {
      Block reply = wallet.getBlock(request);
      if (reply != null) {
        response.getWriter().println(Util.printBlock(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (IllegalArgumentException e) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("Error", e.getMessage());
      response.getWriter().println(jsonObject.toJSONString());
    }
  }

}
