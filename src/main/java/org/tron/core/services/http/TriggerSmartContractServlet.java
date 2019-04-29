package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

import static org.tron.core.services.http.Util.getVisible;
import static org.tron.core.services.http.Util.getVisiblePost;
import static org.tron.core.services.http.Util.setTransactionPermissionId;


@Component
@Slf4j(topic = "API")
public class TriggerSmartContractServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    TriggerSmartContract.Builder build = TriggerSmartContract.newBuilder();
    TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
    Return.Builder retBuilder = Return.newBuilder();
    boolean visible = false;

    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);
      visible = getVisiblePost( contract );
      JsonFormat.merge(contract, build, visible );
      JSONObject jsonObject = JSONObject.parseObject(contract);
      String selector = jsonObject.getString("function_selector");
      String parameter = jsonObject.getString("parameter");
      boolean bHex = true;
      if (jsonObject.containsKey("parameter_string")) {
        parameter = jsonObject.getString("parameter_string");
        bHex = false;
      }
      String data = AbiUtil.parseMethod(selector, parameter, bHex);
      build.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));

      long feeLimit = jsonObject.getLongValue("fee_limit");

      TransactionCapsule trxCap = wallet
          .createTransactionCapsule(build.build(), ContractType.TriggerSmartContract);

      Transaction.Builder txBuilder = trxCap.getInstance().toBuilder();
      Transaction.raw.Builder rawBuilder = trxCap.getInstance().getRawData().toBuilder();
      rawBuilder.setFeeLimit(feeLimit);
      txBuilder.setRawData(rawBuilder);

      Transaction trx = wallet
          .triggerContract(build.build(), new TransactionCapsule(txBuilder.build()), trxExtBuilder,
              retBuilder);
      trx = setTransactionPermissionId(jsonObject, trx);
      trxExtBuilder.setTransaction(trx);
      retBuilder.setResult(true).setCode(response_code.SUCCESS);
    } catch (ContractValidateException e) {
      retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
    } catch (Exception e) {
      retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
    }
    trxExtBuilder.setResult(retBuilder);
    response.getWriter().println(Util.printTransactionExtention(trxExtBuilder.build(), visible));
  }
}