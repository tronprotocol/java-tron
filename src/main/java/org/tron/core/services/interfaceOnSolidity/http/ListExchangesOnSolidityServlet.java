package org.tron.core.services.interfaceOnSolidity.http;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;


@Component
@Slf4j
public class ListExchangesOnSolidityServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;
  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.getWriter().println(JsonFormat.printToString(walletOnSolidity.futureGet(
          () -> wallet.getExchangeList()))
      );
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
    doPost(request, response);
  }
}