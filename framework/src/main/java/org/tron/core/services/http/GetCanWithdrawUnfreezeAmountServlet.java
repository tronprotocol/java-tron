package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetCanWithdrawUnfreezeAmountServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String ownerAddress = request.getParameter("owner_address");
      long timestamp = 0;
      String timestampStr = request.getParameter("timestamp");
      if (timestampStr != null) {
        timestamp = Long.parseLong(timestampStr);
      }
      if (visible) {
        ownerAddress = Util.getHexAddress(ownerAddress);
      }
      fillResponse(visible,
          ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)),
          timestamp,
          response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      GrpcAPI.CanWithdrawUnfreezeAmountRequestMessage.Builder build =
              GrpcAPI.CanWithdrawUnfreezeAmountRequestMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(params.isVisible(),
              build.getOwnerAddress(),
              build.getTimestamp(),
              response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible,
                            ByteString ownerAddress,
                            long timestamp,
                            HttpServletResponse response) throws IOException {
    GrpcAPI.CanWithdrawUnfreezeAmountResponseMessage reply =
            wallet.getCanWithdrawUnfreezeAmount(ownerAddress, timestamp);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
