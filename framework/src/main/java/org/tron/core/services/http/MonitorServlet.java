package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.monitor.MonitorService;
import org.tron.protos.Protocol;

@Component
@Slf4j(topic = "API")
public class MonitorServlet extends RateLimiterServlet {

  @Autowired
  MonitorService monitorService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      //response.getWriter().println("{\"status\":0,\"msg\":\"\",\"data\":{\"interval\":60,\"forkCount\":0,\"errorProtoCount\":0,\"blockchain\":{\"nodeIp\":\"127.0.0.1\",\"nodeType\":1,\"nodeStatus\":1,\"nodeVersion\":\"3.6.5\",\"outdatedSRCount\":0,\"outdatedSRList\":[{\"address\":\"41d376d829440505ea13c9d1c455317d51b62e4ab6\",\"url\":\"http://blockchain.org\",\"version\":\"3.6.5\"}],\"blockGeneratingTime\":1,\"TxProcessingTime\":1,\"blockHeight\":111,\"discardBlockCount\":1,\"blockDelayInfo\":{\"delay1s\":1,\"delay2s\":1,\"delay3s\":1},\"TxFrequency\":{\"oneSecond\":1,\"oneMinute\":1,\"fiveMinute\":1},\"TxQueueSize\":1,\"unConfirmedTx\":1},\"net\":{\"httpTotalCount\":0,\"httpFailCount\":0,\"httpEndpointRequest\":[{\"endpoint\":\"wallet/getnodeinfo\",\"count\":1,\"failCount\":0}],\"connectionCount\":1,\"validCount\":1,\"TCPDisconnectionCount\":1,\"TCPInTraffic\":11,\"TCPOutTraffic\":11,\"TCPDisconnectionDetail\":[{\"reason\":\"TOO_MANY_PEERS\",\"count\":1}],\"UDPInTraffic\":11,\"UDPOutTraffic\":11,\"latency\":[{\"witnessAddress\":\"41d376d829440505ea13c9d1c455317d51b62e4ab6\",\"delayDetails\":{\"TOP99\":11,\"TOP95\":10,\"totalCount\":111,\"delay1S\":11,\"delay2S\":12,\"delay3S\":13}}]}}}\n");
      Protocol.MonitorInfo monitorInfo = monitorService.getDefaultInfo();
      if (monitorInfo != null) {
        response.getWriter().println(JsonFormat.printToString(monitorInfo, true));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
