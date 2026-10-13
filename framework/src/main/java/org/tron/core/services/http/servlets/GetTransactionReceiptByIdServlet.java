package org.tron.core.services.http.servlets;

import com.google.protobuf.ByteString;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.protos.Protocol.TransactionInfo;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "gettransactionreceiptbyid", access = Access.READ,
    surfaces = {Surface.FULL})
public class GetTransactionReceiptByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("value");
      TransactionInfo result = wallet
          .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(input)));

      if (result != null) {
        response.getWriter().println(
            Util.printTransactionFee(JsonFormat.printToString(result, visible)));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      TransactionInfo result = wallet.getTransactionInfoById(build.getValue());

      if (result != null) {
        response.getWriter().println(
            Util.printTransactionFee(JsonFormat
                .printToString(result, params.isVisible())));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}