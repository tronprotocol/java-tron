package org.tron.core.services.interfaceOnSolidity.http.solidity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;
import org.tron.core.services.http.GetTransactionByIdServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j(topic = "API")
public class GetTransactionByIdOnSolidityServlet
    extends GetTransactionByIdServlet {

  @Autowired
  private Manager dbManager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    dbManager.declareSolidity();
    super.doGet(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    dbManager.declareSolidity();
    super.doPost(request, response);
  }
}