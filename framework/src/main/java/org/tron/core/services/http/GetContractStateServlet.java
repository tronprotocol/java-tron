package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;
import org.tron.protos.contract.SmartContractOuterClass;

@Component
@Slf4j(topic = "API")
public class GetContractStateServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  private static final String VALUE = "value";

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter(VALUE);
      if (visible) {
        input = Util.getHexAddress(input);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(VALUE, input);
      GrpcAPI.BytesMessage.Builder build = GrpcAPI.BytesMessage.newBuilder();
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);
      SmartContractOuterClass.ContractState contractState = wallet.getContractState(build.build());

      if (contractState == null) {
        response.getWriter().println("{}");
      } else {
        JSONObject jsonContractState = JSONObject
            .parseObject(JsonFormat.printToString(contractState, visible));
        response.getWriter().println(jsonContractState.toJSONString());
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      String input = params.getParams();
      boolean visible = params.isVisible();
      if (visible) {
        JSONObject jsonObject = JSONObject.parseObject(input);
        String value = jsonObject.getString(VALUE);
        jsonObject.put(VALUE, Util.getHexAddress(value));
        input = jsonObject.toJSONString();
      }

      GrpcAPI.BytesMessage.Builder build = GrpcAPI.BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      SmartContractOuterClass.ContractState contractState = wallet.getContractState(build.build());

      if (contractState == null) {
        response.getWriter().println("{}");
      } else {
        JSONObject jsonContractState = JSONObject
            .parseObject(JsonFormat.printToString(contractState, visible));
        response.getWriter().println(jsonContractState.toJSONString());
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
