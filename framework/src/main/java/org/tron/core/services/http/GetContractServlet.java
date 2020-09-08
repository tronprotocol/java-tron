package org.tron.core.services.http;

import static org.tron.core.services.http.PostParams.S_VALUE;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.core.Wallet;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;


@Component
@Slf4j(topic = "API")
public class GetContractServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter(S_VALUE);
      if (visible) {
        input = Util.getHexAddress(input);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(S_VALUE, input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);
      SmartContract smartContract = wallet.getContract(build.build());
      JSONObject jsonSmartContract = JSONObject
          .parseObject(JsonFormat.printToString(smartContract, visible));
      response.getWriter().println(jsonSmartContract.toJSONString());
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      boolean visible = params.isVisible();
      String input = params.getParams();
      if (visible) {
        JSONObject jsonObject = JSONObject.parseObject(input);
        String value = jsonObject.getString(S_VALUE);
        jsonObject.put(S_VALUE, Util.getHexAddress(value));
        input = jsonObject.toJSONString();
      }

      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      SmartContract smartContract = wallet.getContract(build.build());
      JSONObject jsonSmartContract = JSONObject
          .parseObject(JsonFormat.printToString(smartContract, visible));
      response.getWriter().println(jsonSmartContract.toJSONString());
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
