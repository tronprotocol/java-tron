package org.tron.core.services.http.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.NodeInfo;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.json.JSON;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getnodeinfo", access = Access.READ,
    surfaces = {Surface.FULL, Surface.SOLIDITY, Surface.PBFT, Surface.SOLIDITY_NODE})
public class GetNodeInfoServlet extends RateLimiterServlet {

  @Autowired
  private NodeInfoService nodeInfoService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
      response.getWriter().println(JSON.toJSONString(nodeInfo));

    } catch (Exception e) {
      Util.processServerError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
