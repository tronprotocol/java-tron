package org.tron.core.services.http;

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


@Component
@Slf4j(topic = "API")
public class GetDelegatedResourceServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String fromAddress = request.getParameter("fromAddress");
      String toAddress = request.getParameter("toAddress");
      if (visible) {
        fromAddress = Util.getHexAddress(fromAddress);
        toAddress = Util.getHexAddress(toAddress);
      }

      DelegatedResourceList reply =
          wallet.getDelegatedResource(
              ByteString.copyFrom(ByteArray.fromHexString(fromAddress)),
              ByteString.copyFrom(ByteArray.fromHexString(toAddress)));
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
      DelegatedResourceMessage.Builder build = DelegatedResourceMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      DelegatedResourceList reply =
          wallet.getDelegatedResource(build.getFromAddress(), build.getToAddress());
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
