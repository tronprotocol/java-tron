package org.tron.core.services.http.servlets;

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
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getcandelegatedmaxsize", access = Access.READ,
    surfaces = {Surface.FULL, Surface.SOLIDITY, Surface.PBFT, Surface.SOLIDITY_NODE})
public class GetCanDelegatedMaxSizeServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      int type = 0;
      String typeStr = request.getParameter("type");
      if (typeStr != null) {
        type = Integer.parseInt(typeStr);
      }
      String ownerAddress = request.getParameter("owner_address");
      if (visible) {
        ownerAddress = Util.getHexAddress(ownerAddress);
      }
      fillResponse(visible,
              ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)),
              type, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      GrpcAPI.CanDelegatedMaxSizeRequestMessage.Builder build =
              GrpcAPI.CanDelegatedMaxSizeRequestMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(params.isVisible(),
              build.getOwnerAddress(),
              build.getType(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible,
                            ByteString ownerAddress,
                            int resourceType,
                            HttpServletResponse response) throws IOException {
    GrpcAPI.CanDelegatedMaxSizeResponseMessage reply =
            wallet.getCanDelegatedMaxSize(ownerAddress, resourceType);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
