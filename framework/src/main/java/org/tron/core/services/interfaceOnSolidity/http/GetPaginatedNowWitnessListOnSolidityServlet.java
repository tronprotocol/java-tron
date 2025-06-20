package org.tron.core.services.interfaceOnSolidity.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.GetPaginatedNowWitnessListServlet;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j(topic = "API")
public class GetPaginatedNowWitnessListOnSolidityServlet extends GetPaginatedNowWitnessListServlet {
    @Autowired
    private WalletOnSolidity walletOnSolidity;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        walletOnSolidity.futureGet(() -> super.doGet(request, response));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        walletOnSolidity.futureGet(() -> super.doPost(request, response));
    }
}
