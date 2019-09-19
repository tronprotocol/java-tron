package org.tron.core.services.interfaceOnSolidity.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.ListWitnessesServlet;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.SolidityHttpRequest;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

import java.io.IOException;


@Component
@Slf4j(topic = "API")
public class ListWitnessesOnSolidityServlet extends HttpServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;
  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      GrpcAPI.WitnessList reply = walletOnSolidity.futureGet(() -> wallet.getWitnessList());
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
    doGet(request, response);
  }
}
