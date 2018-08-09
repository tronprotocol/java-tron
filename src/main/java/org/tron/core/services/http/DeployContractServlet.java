package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class DeployContractServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      CreateSmartContract.Builder build = CreateSmartContract.newBuilder();
      JSONObject jsonObject = JSONObject.parseObject(contract);
      byte[] ownerAddress = ByteArray.fromHexString(jsonObject.getString("owner_address"));
      build.setOwnerAddress(ByteString.copyFrom(ownerAddress));

      String abi = jsonObject.getString("abi");
      StringBuffer abiSB = new StringBuffer("{");
      abiSB.append("\"entrys\":");
      abiSB.append(abi);
      abiSB.append("}");
      ABI.Builder abiBuilder = ABI.newBuilder();
      JsonFormat.merge(abiSB.toString(), abiBuilder);

      long feeLimit = jsonObject.getLongValue("fee_limit");

      SmartContract.Builder smartBuilder = SmartContract.newBuilder();
      smartBuilder.setAbi(abiBuilder)
          .setCallValue(jsonObject.getLongValue("call_value"))
          .setConsumeUserResourcePercent(jsonObject.getLongValue("consume_user_resource_percent"));
      if (!ArrayUtils.isEmpty(ownerAddress)) {
        smartBuilder.setOriginAddress(ByteString.copyFrom(ownerAddress));
      }

      byte[] byteCode = ByteArray.fromHexString(jsonObject.getString("bytecode"));
      if (!ArrayUtils.isEmpty(byteCode)) {
        smartBuilder.setBytecode(ByteString.copyFrom(byteCode));
      }
      String name = jsonObject.getString("name");
      if (!Strings.isNullOrEmpty(name)) {
        smartBuilder.setName(name);
      }

      build.setNewContract(smartBuilder);
      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.CreateSmartContract).getInstance();
      Transaction.Builder txBuilder = tx.toBuilder();
      Transaction.raw.Builder rawBuilder = tx.getRawData().toBuilder();
      rawBuilder.setFeeLimit(feeLimit);
      txBuilder.setRawData(rawBuilder);
      response.getWriter().println(Util.printTransaction(txBuilder.build()));
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}