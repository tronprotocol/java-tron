package org.tron.core.services.http;

import java.io.IOException;
import org.tron.common.utils.ByteArray;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;

@Component
@Slf4j
public class GenerateShieldAddressServlet extends HttpServlet {

  @Autowired private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      Map<String, byte[]> reply = wallet.generateShieldAddress();
      if (reply != null) {
        response
            .getWriter()
            .println(
                "{\"private_key\": \""
                    + ByteArray.toHexString(reply.get("private_key"))
                    + "\","
                    + "\"public_key\": \""
                    + ByteArray.toHexString(reply.get("public_key"))
                    + "\"}");
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
