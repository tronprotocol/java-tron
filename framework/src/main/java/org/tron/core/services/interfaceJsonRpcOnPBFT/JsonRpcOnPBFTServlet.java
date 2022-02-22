package org.tron.core.services.interfaceJsonRpcOnPBFT;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;
import org.tron.core.services.jsonrpc.JsonRpcServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcOnPBFTServlet extends JsonRpcServlet {

  @Autowired
  private WalletOnPBFT walletOnPBFT;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnPBFT.futureGet(() -> {
      try {
        super.doPost(request, response);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
  }
}