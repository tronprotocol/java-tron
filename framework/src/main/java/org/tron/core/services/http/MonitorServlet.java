package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "API")
public class MonitorServlet extends RateLimiterServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.getWriter().println("{\"status\":0,\"msg\":\"\",\"data\":{\"interval\":60,\"forkCount\":0,\"errorProtoCount\":0,\"node\":{\"ip\":\"\",\"type\":1,\"status\":1,\"version\":\"\",\"outdatedSR\":0},\"http\":{\"requestCount\":0,\"errorCount\":0,\"endpointRequest\":[{\"endpoint\":\"\",\"count\":1,\"failCount\":0}]},\"transaction\":{\"TPS\":1,\"TxQueueSize\":1,\"unconfirmedTx\":1},\"block\":{\"generatingTime\":1,\"txProcessingTime\":1,\"height\":111,\"discardBlockCount\":1,\"delayInfo\":{\"delay1s\":1,\"delay2s\":1,\"delay3s\":1}},\"p2p\":{\"connectionCount\":1,\"valideCount\":1,\"txIn\":11,\"txOut\":11},\"TCP\":{\"successConnectionCount\":1,\"errorConnectionCount\":1,\"disconnectionCount\":1,\"inFlow\":11,\"outFlow\":11},\"UDP\":{\"inFlow\":11,\"outFlow\":11}}}\n");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
