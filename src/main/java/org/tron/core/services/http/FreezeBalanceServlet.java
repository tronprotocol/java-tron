package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class FreezeBalanceServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      String contract = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      FreezeBalanceContract.Builder build = FreezeBalanceContract.newBuilder();
      JsonFormat.merge(contract, build);
      Transaction tx = wallet.createTransactionCapsule(build.build(), ContractType.FreezeBalanceContract).getInstance();
      String txid = ByteArray.toHexString(Sha256Hash.hash(tx.getRawData().toByteArray()));
      response.getWriter().println(JsonFormat.printToString(tx) + "\n\n\ntxid=" + txid);
    } catch (ContractValidateException e) {
      logger.debug("ContractValidateException: {}", e.getMessage());
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}
