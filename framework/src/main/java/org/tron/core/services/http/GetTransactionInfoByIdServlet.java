package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;


@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  private static String convertLogAddressToTronAddress(TransactionInfo transactionInfo,
      boolean visible) {
    if (visible) {
      List<Log> newLogList = Util.convertLogAddressToTronAddress(transactionInfo);
      transactionInfo = transactionInfo.toBuilder().clearLog().addAllLog(newLogList).build();
    }
    return JsonFormat.printToString(transactionInfo, visible);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("value");
      TransactionInfo reply = wallet
          .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(input)));
      if (reply != null) {
        response.getWriter().println(convertLogAddressToTronAddress(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      TransactionInfo reply = wallet.getTransactionInfoById(build.getValue());
      if (reply != null) {
        response.getWriter().println(convertLogAddressToTronAddress(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}