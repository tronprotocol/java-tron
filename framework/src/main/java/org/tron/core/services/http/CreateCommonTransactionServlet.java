package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j(topic = "API")
public class CreateCommonTransactionServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      String contract = params.getParams();
      boolean visible = params.isVisible();
      ContractType type = ContractType.valueOf(Util.getContractType(contract));
      Message.Builder build = getBuilder(type);
      JsonFormat.merge(contract, build, visible);
      Transaction tx = wallet.createTransactionCapsule(build.build(), type).getInstance();
      JSONObject jsonObject = JSONObject.parseObject(contract);
      tx = Util.setTransactionPermissionId(jsonObject, tx);
      response.getWriter().println(Util.printCreateTransaction(tx, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private Message.Builder getBuilder(ContractType type) throws NoSuchMethodException,
      IllegalAccessException, InvocationTargetException, InstantiationException,
      ContractValidateException {
    Class clazz = TransactionFactory.getContract(type);
    if (clazz != null) {
      Constructor<GeneratedMessageV3> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      GeneratedMessageV3 generatedMessageV3 = constructor.newInstance();
      return generatedMessageV3.toBuilder();
    } else {
      throw new ContractValidateException("don't have this type: " + type);
    }
  }
}
