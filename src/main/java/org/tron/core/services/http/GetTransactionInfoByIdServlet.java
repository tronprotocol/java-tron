package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.runtime.utils.MUtil;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

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
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
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
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  private static String convertLogAddressToTronAddress(TransactionInfo transactionInfo,
      boolean visible) {
    if (visible) {
      List<Log> newLogList = new ArrayList<>();
      for (Log log : transactionInfo.getLogList()) {
        Log.Builder logBuilder = Log.newBuilder();
        logBuilder.setData(log.getData());
        logBuilder.addAllTopics(log.getTopicsList());

        byte[] oldAddress = log.getAddress().toByteArray();
        if (oldAddress.length == 0 || oldAddress.length > 20) {
          logBuilder.setAddress(log.getAddress());
        } else {
          byte[] newAddress = new byte[20];

          int start = 20 - oldAddress.length;
          System.arraycopy(oldAddress, 0, newAddress, start, oldAddress.length);
          logBuilder.setAddress(ByteString.copyFrom(MUtil.convertToTronAddress(newAddress)));
        }
        newLogList.add(logBuilder.build());
      }
      transactionInfo = transactionInfo.toBuilder().clearLog().addAllLog(newLogList).build();
    }
    return JsonFormat.printToString(transactionInfo, visible);
  }
}