package org.tron.core.services.interfaceOnSolidity.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetAssetIssueListByNameOnSolidityServlet extends HttpServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;
  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("value");
      if (visible) {
        input = Util.getHexString(input);
      }
      String finalInput = input;
      GrpcAPI.AssetIssueList reply =
              walletOnSolidity.futureGet(() -> wallet.getAssetIssueListByName(ByteString.copyFrom(ByteArray.fromHexString(finalInput))));
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, visible));
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
      JSONObject jsonObject = JSON.parseObject(input);
      String value = jsonObject.getString("value");
      if (visible) {
        value = Util.getHexString(value);
      }
      String finalValue = value;
      GrpcAPI.AssetIssueList reply =
              walletOnSolidity.futureGet(
                      () -> wallet.getAssetIssueListByName(
                              ByteString.copyFrom(ByteArray.fromHexString(finalValue))));
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, visible));
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