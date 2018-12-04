package org.tron.core.services.interfaceOnSolidity.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;


@Component
@Slf4j
public class GetExchangeByIdOnSolidityServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;
  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long id = jsonObject.getLong("id");
      response.getWriter()
          .println(JsonFormat
              .printToString(walletOnSolidity.futureGet(
                  () -> wallet.getExchangeById(ByteString.copyFrom(ByteArray.fromLong(id))))
              ));
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getParameter("id");
      response.getWriter()
          .println(JsonFormat.printToString(walletOnSolidity.futureGet(
              () -> wallet.getExchangeById(
                  ByteString.copyFrom(ByteArray.fromLong(Long.parseLong(input)))))));
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