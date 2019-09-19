package org.tron.core.services.interfaceOnSolidity.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.GetDelegatedResourceServlet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.SolidityHttpRequest;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetDelegatedResourceOnSolidityServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String fromAddress = request.getParameter("fromAddress");
      String toAddress = request.getParameter("toAddress");
      if (visible) {
        fromAddress = Util.getHexAddress(fromAddress);
        toAddress = Util.getHexAddress(toAddress);
      }

      String finalFromAddress = fromAddress;
      String finalToAddress = toAddress;
      GrpcAPI.DelegatedResourceList reply =
              walletOnSolidity.futureGet(() -> wallet.getDelegatedResource(
                      ByteString.copyFrom(ByteArray.fromHexString(finalFromAddress)),
                      ByteString.copyFrom(ByteArray.fromHexString(finalToAddress))));
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
      String input =
              request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      GrpcAPI.DelegatedResourceMessage.Builder build = GrpcAPI.DelegatedResourceMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      GrpcAPI.DelegatedResourceList reply =
              walletOnSolidity.futureGet(
                      () -> wallet.getDelegatedResource(build.getFromAddress(), build.getToAddress()));
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
