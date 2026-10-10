package org.tron.core.services.http.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.services.http.HttpApiExcluded;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;

@Component
@Slf4j(topic = "API")
@HttpApiExcluded("sapling shielded note-scan API, disabled on every surface")
public class GetMerkleTreeVoucherInfoServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      OutputPointInfo.Builder build = OutputPointInfo.newBuilder();
      JsonFormat.merge(params.getParams(), build);
      IncrementalMerkleVoucherInfo reply = wallet.getMerkleTreeVoucherInfo(build.build());
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, params.isVisible()));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
