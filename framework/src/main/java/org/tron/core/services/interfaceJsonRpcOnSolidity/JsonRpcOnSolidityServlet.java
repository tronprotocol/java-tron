package org.tron.core.services.interfaceJsonRpcOnSolidity;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;
import org.tron.core.services.jsonrpc.JsonRpcServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcOnSolidityServlet extends JsonRpcServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> {
      try {
        super.doPost(request, response);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
  }
}