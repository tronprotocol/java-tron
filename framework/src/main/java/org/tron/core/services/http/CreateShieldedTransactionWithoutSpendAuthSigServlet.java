package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.PrivateParametersWithoutAsk;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;


@Component
@Slf4j(topic = "API")
public class CreateShieldedTransactionWithoutSpendAuthSigServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);
      boolean visible = Util.getVisiblePost(contract);

      PrivateParametersWithoutAsk.Builder build = PrivateParametersWithoutAsk.newBuilder();
      JsonFormat.merge(contract, build, visible);

      Transaction tx = wallet
          .createShieldedTransactionWithoutSpendAuthSig(build.build())
          .getInstance();

      String txString = Util.printCreateTransaction(tx, visible);
      JSONObject jsonObject = JSON.parseObject(txString);
      if (jsonObject.containsKey("txID")) {
        jsonObject.remove("txID");
      }

      response.getWriter().println(jsonObject.toJSONString());
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
