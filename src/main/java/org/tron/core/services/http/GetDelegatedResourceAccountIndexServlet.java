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
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;

import static org.tron.core.services.http.Util.getVisible;

@Component
@Slf4j(topic = "API")
public class GetDelegatedResourceAccountIndexServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = getVisible(request);
      String address = request.getParameter("value");
      DelegatedResourceAccountIndex reply =
          wallet.getDelegatedResourceAccountIndex(
              ByteString.copyFrom(ByteArray.fromHexString(address)));
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, visible ));
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
      boolean visible = getVisible(request);
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build);
      DelegatedResourceAccountIndex reply =
          wallet.getDelegatedResourceAccountIndex(build.getValue());
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, visible ));
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
