package org.tron.core.services.interfaceOnSolidity.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.GetPaginatedAssetIssueListServlet;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;


@Component
@Slf4j
public class GetPaginatedAssetIssueListOnSolidityServlet
    extends GetPaginatedAssetIssueListServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> super.doPost(request, response));
  }
}
