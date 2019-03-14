package org.tron.core.services.interfaceOnSolidity.http.solidity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.services.http.GetDeferredTransactionInfoByIdServlet;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

public class GetDeferredTransactionInfoByIdOnSolidyServlet extends
    GetDeferredTransactionInfoByIdServlet {
  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> super.doGet(request, response));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> super.doPost(request, response));
  }
}
