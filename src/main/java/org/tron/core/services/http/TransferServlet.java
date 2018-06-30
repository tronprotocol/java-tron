package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javassist.bytecode.stackmap.BasicBlock.Catch;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.TransferContract.Builder;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class TransferServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      String contract = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Builder build = TransferContract.newBuilder();
      JsonFormat.merge(contract, build);
      Transaction tx = wallet.createTransactionCapsule(build.build(), ContractType.TransferContract).getInstance();
      response.getWriter().println(JsonFormat.printToString(tx));
    } catch (ContractValidateException e) {
      logger.debug("ContractValidateException: {}", e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}
