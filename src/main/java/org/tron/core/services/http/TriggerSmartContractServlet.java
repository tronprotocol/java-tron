package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class TriggerSmartContractServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  public static String parseMethod(String methodSign, String params) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (params.length() == 0) {
      return Hex.toHexString(selector);
    }
    String result = Hex.toHexString(selector) + params;
    return result;
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    TriggerSmartContract.Builder build = TriggerSmartContract.newBuilder();
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JsonFormat.merge(contract, build);
      JSONObject jsonObject = JSONObject.parseObject(contract);
      String selector = jsonObject.getString("function_selector");
      String parameter = jsonObject.getString("parameter");
      String data = parseMethod(selector, parameter);
      build.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));

      long storageLimit = jsonObject.getLongValue("storage_limit");
      long dropLimit = jsonObject.getLongValue("drop_limit");
      long cpuLimit = jsonObject.getLongValue("cpu_limit");
      long bandwidthLimit = jsonObject.getLongValue("bandwidth_limit");

      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.TriggerSmartContract).getInstance();
      Transaction.Builder txBuilder = tx.toBuilder();
      Transaction.raw.Builder rawBuilder = tx.getRawData().toBuilder();
      rawBuilder.setMaxCpuUsage(cpuLimit);
      rawBuilder.setMaxNetUsage(bandwidthLimit);
      rawBuilder.setMaxStorageUsage(storageLimit);
      txBuilder.setRawData(rawBuilder);

      Transaction trx = wallet.triggerContract(build.build(), new TransactionCapsule(txBuilder.build()));
      response.getWriter().println(Util.printTransaction(trx));

    } catch (Exception e) {
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }

  }
}