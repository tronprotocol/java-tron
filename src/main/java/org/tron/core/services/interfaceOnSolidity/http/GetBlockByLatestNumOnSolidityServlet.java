package org.tron.core.services.interfaceOnSolidity.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;
import org.tron.core.services.http.GetBlockByLatestNumServlet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.SolidityHttpRequest;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetBlockByLatestNumOnSolidityServlet extends HttpServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;
  @Autowired
  private Wallet wallet;
  private static final long BLOCK_LIMIT_NUM = 100;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long getNum = Long.parseLong(request.getParameter("num"));
      if (getNum > 0 && getNum < BLOCK_LIMIT_NUM) {
        GrpcAPI.BlockList reply = walletOnSolidity.futureGet(() -> wallet.getBlockByLatestNum(getNum));
        if (reply != null) {
          response.getWriter().println(Util.printBlockList(reply, visible));
          return;
        }
      }
      response.getWriter().println("{}");
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
      GrpcAPI.NumberMessage.Builder build = GrpcAPI.NumberMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      long getNum = build.getNum();
      if (getNum > 0 && getNum < BLOCK_LIMIT_NUM) {
        GrpcAPI.BlockList reply = walletOnSolidity.futureGet(() -> wallet.getBlockByLatestNum(getNum));
        if (reply != null) {
          response.getWriter().println(Util.printBlockList(reply, visible));
          return;
        }
      }
      response.getWriter().println("{}");
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
