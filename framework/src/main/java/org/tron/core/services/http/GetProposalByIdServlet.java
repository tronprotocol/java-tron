package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Proposal;


@Component
@Slf4j(topic = "API")
public class GetProposalByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("id");
      long id = new Long(input);
      fillResponse(ByteString.copyFrom(ByteArray.fromLong(id)), visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      long id = Util.getJsonLongValue(jsonObject, "id", true);
      fillResponse(ByteString.copyFrom(ByteArray.fromLong(id)), params.isVisible(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(ByteString proposalId, boolean visible, HttpServletResponse response)
      throws IOException {
    Proposal reply = wallet.getProposalById(proposalId);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}