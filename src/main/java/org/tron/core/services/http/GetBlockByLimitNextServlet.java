package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.core.Wallet;

@Component
@Slf4j
public class GetBlockByLimitNextServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;
  private static final long BLOCK_LIMIT_NUM = 100;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long startNum = Long.parseLong(request.getParameter("startNum"));
      long endNum = Long.parseLong(request.getParameter("endNum"));
      if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
        BlockList reply = wallet.getBlocksByLimitNext(startNum, endNum - startNum);
        if (reply != null) {
          response.getWriter().println(Util.printBlockList(reply));
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
      BlockLimit.Builder build = BlockLimit.newBuilder();
      JsonFormat.merge(input, build);
      long startNum = build.getStartNum();
      long endNum = build.getEndNum();
      if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
        BlockList reply = wallet.getBlocksByLimitNext(startNum, endNum - startNum);
        if (reply != null) {
          response.getWriter().println(JsonFormat.printToString(reply));
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