package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.BalanceContract.BlockBalanceTrace;


@Component
@Slf4j(topic = "API")
public class GetBlockBalanceServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      BlockBalanceTrace.BlockIdentifier.Builder builder = BlockBalanceTrace.BlockIdentifier
          .newBuilder();
      JsonFormat.merge(params.getParams(), builder, params.isVisible());
      fillResponse(params.isVisible(), builder.build(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, BlockBalanceTrace.BlockIdentifier request,
                            HttpServletResponse response)
      throws Exception {
    BlockBalanceTrace reply = wallet.getBlockBalance(request);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
