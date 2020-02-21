package org.tron.core.services.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.core.vm.utils.MUtil;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByBlockNumServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  private JSONObject convertLogAddressToTronAddress(TransactionInfo transactionInfo,
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

    return JSONObject.parseObject(JsonFormat.printToString(transactionInfo, visible));
  }

  private String printTransactionInfoList(TransactionInfoList list, boolean selfType) {
    JSONArray jsonArray = new JSONArray();
    for (TransactionInfo transactionInfo : list.getTransactionInfoList()) {
      jsonArray.add(convertLogAddressToTronAddress(transactionInfo, selfType));
    }
    return jsonArray.toJSONString();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long num = Long.parseLong(request.getParameter("num"));

      if (num > 0L) {
        TransactionInfoList reply = wallet.getTransactionInfoByBlockNum(num);
        response.getWriter().println(printTransactionInfoList(reply, visible));
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

      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(input, build, visible);

      long num = build.getNum();
      if (num > 0L) {
        TransactionInfoList reply = wallet.getTransactionInfoByBlockNum(num);
        response.getWriter().println(printTransactionInfoList(reply, visible));
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
}
