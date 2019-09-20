package org.tron.core.services.interfaceOnSolidity.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;
import org.tron.core.services.http.TriggerConstantContractServlet;


@Component
@Slf4j(topic = "API")
public class TriggerConstantContractOnSolidityServlet extends TriggerConstantContractServlet {

  @Autowired
  private Manager dbManager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    dbManager.declareSolidity();
    super.doGet(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      dbManager.declareSolidity();
      super.doPost(request, response);
    } catch (IOException e) {
      logger.error("TriggerConstantContractOnSolidityServlet Exception", e);
    }
  }

}
