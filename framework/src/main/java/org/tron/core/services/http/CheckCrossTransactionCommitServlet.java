package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.ibc.common.CrossChainService;


@Component
@Slf4j(topic = "API")
public class CheckCrossTransactionCommitServlet extends RateLimiterServlet {

  @Autowired
  private CrossChainService crossChainService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      ByteString txId = getTxId(request);
      boolean value = crossChainService.checkCrossChainCommit(txId);
      response.getWriter().println("{\"result\": " + value + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }

  private ByteString getTxId(HttpServletRequest request) throws Exception {
    ByteString txId = null;
    String txIdStr = request.getParameter("txId");
    if (StringUtils.isBlank(txIdStr)) {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      txId = build.getValue();
    } else {
      ByteString.copyFrom(ByteArray.fromHexString(txIdStr));
    }
    return txId;
  }
}
