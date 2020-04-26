package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
      PostParams params = PostParams.getPostParams(request);
      PrivateParametersWithoutAsk.Builder build = PrivateParametersWithoutAsk.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      Transaction tx = wallet
          .createShieldedTransactionWithoutSpendAuthSig(build.build())
          .getInstance();
      String txString = Util.printCreateTransaction(tx, params.isVisible());
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
