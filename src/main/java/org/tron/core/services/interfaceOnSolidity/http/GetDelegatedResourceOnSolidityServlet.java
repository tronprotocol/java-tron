package org.tron.core.services.interfaceOnSolidity.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.DelegatedResourceMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

@Component
@Slf4j
public class GetDelegatedResourceOnSolidityServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String fromAddress = request.getParameter("fromAddress");
      String toAddress = request.getParameter("toAddress");

      DelegatedResourceList reply = walletOnSolidity.futureGet(
          () -> wallet
              .getDelegatedResource(ByteString.copyFrom(ByteArray.fromHexString(fromAddress)),
                  ByteString.copyFrom(ByteArray.fromHexString(toAddress)))
      );
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply));
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
      String input =
          request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      DelegatedResourceMessage.Builder build = DelegatedResourceMessage.newBuilder();
      JsonFormat.merge(input, build);
      DelegatedResourceList reply = walletOnSolidity.futureGet(
          () -> wallet.getDelegatedResource(build.getFromAddress(), build.getToAddress())
      );
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply));
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
