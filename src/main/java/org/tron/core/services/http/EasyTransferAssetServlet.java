package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EasyTransferAssetMessage;
import org.tron.api.GrpcAPI.EasyTransferResponse;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class EasyTransferAssetServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
    EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      EasyTransferAssetMessage.Builder build = EasyTransferAssetMessage.newBuilder();
      JsonFormat.merge(input, build);
      byte[] privateKey = wallet.pass2Key(build.getPassPhrase().toByteArray());
      ECKey ecKey = ECKey.fromPrivate(privateKey);
      byte[] owner = ecKey.getAddress();
      TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(owner));
      builder.setToAddress(build.getToAddress());
      builder.setAssetName(ByteString.copyFrom(build.getAssetId().getBytes()));
      builder.setAmount(build.getAmount());

      TransactionCapsule transactionCapsule;
      transactionCapsule = wallet
          .createTransactionCapsule(builder.build(), ContractType.TransferAssetContract);
      transactionCapsule.sign(privateKey);
      GrpcAPI.Return retur = wallet.broadcastTransaction(transactionCapsule.getInstance());
      responseBuild.setTransaction(transactionCapsule.getInstance());
      responseBuild.setResult(retur);
      response.getWriter().println(Util.printEasyTransferResponse(responseBuild.build()));
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
      returnBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build()));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
      returnBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build()));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    } catch (ContractValidateException e) {
      returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build()));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    }
  }
}
