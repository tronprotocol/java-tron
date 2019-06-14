package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;

public class BroadcastHexServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String input = request.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));
    String trx = JSONObject.parseObject(input).getString("transaction");
    Transaction transaction = Transaction.parseFrom(ByteArray.fromHexString(trx));

    GrpcAPI.Return result = wallet.broadcastTransaction(transaction);
    JSONObject json = new JSONObject();
    json.put("success", result.getResult());
    json.put("code", result.getCode().toString());
    json.put("message", result.getMessage().toStringUtf8());
    json.put("transaction", "");

    response.getWriter().println(json.toJSONString());
  }
}
