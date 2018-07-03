package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
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
import org.tron.protos.Contract.WitnessUpdateContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class UpdateWitnessServlet  extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      WitnessUpdateContract.Builder build = WitnessUpdateContract.newBuilder();
      JsonFormat.merge(contract, build);
      Transaction tx = wallet.createTransactionCapsule(build.build(), ContractType.AssetIssueContract).getInstance();
      response.getWriter().println(Util.printTransaction(tx));
    } catch (ContractValidateException e) {
      logger.debug("ContractValidateException: {}", e.getMessage());
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}