package org.tron.core.services.interfaceOnPBFT.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.TriggerConstantContractServlet;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;


@Component
@Slf4j(topic = "API")
public class TriggerConstantContractOnPBFTServlet extends TriggerConstantContractServlet {

  @Autowired
  private WalletOnPBFT walletOnPBFT;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    walletOnPBFT.futureGet(() -> super.doGet(request, response));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnPBFT.futureGet(() -> {
      try {
        super.doPost(request, response);
      } catch (IOException e) {
        logger.error("TriggerConstantContractOnPBFTServlet Exception", e);
      }
    });
  }
}
