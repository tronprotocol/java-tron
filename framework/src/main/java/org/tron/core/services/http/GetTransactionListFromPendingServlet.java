package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Transaction;


@Component
@Slf4j(topic = "API")
public class GetTransactionListFromPendingServlet extends RateLimiterServlet {

  @Autowired
  private Manager manager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      Collection<String> result = manager.getTxListFromPending();
      TransactionIdList.Builder builder = TransactionIdList.newBuilder();
      builder.addAllTxId(result);
      response.getWriter().println(Util.printTransactionList(builder.build(), visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      Collection<String> result = manager.getTxListFromPending();
      TransactionIdList.Builder builder = TransactionIdList.newBuilder();
      builder.addAllTxId(result);
      response.getWriter()
          .println(Util.printTransactionList(builder.build(), params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
