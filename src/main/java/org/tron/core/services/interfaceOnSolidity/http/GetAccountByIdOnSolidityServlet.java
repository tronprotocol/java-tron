package org.tron.core.services.interfaceOnSolidity.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.services.http.GetAccountByIdServlet;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceOnSolidity.SolidityHttpRequest;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;
import org.tron.protos.Protocol;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetAccountByIdOnSolidityServlet extends HttpServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;
  @Autowired
  private Wallet wallet;

  private String convertOutput(Protocol.Account account) {
    // convert asset id
    if (account.getAssetIssuedID().isEmpty()) {
      return JsonFormat.printToString(account, false);
    } else {
      JSONObject accountJson = JSONObject.parseObject(JsonFormat.printToString(account, false));
      String assetId = accountJson.get("asset_issued_ID").toString();
      accountJson.put(
              "asset_issued_ID", ByteString.copyFrom(ByteArray.fromHexString(assetId)).toStringUtf8());
      return accountJson.toJSONString();
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String accountId = request.getParameter("account_id");
      Protocol.Account.Builder build = Protocol.Account.newBuilder();
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("account_id", accountId);
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);

      Protocol.Account reply = walletOnSolidity.futureGet(() -> wallet.getAccountById(build.build()));
      if (reply != null) {
        if (visible) {
          response.getWriter().println(JsonFormat.printToString(reply, true));
        } else {
          response.getWriter().println(convertOutput(reply));
        }
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String account = request.getReader().lines()
              .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(account);
      boolean visible = Util.getVisiblePost(account);
      Protocol.Account.Builder build = Protocol.Account.newBuilder();
      JsonFormat.merge(account, build, visible);

      Protocol.Account reply = walletOnSolidity.futureGet(() -> wallet.getAccountById(build.build()));
      if (reply != null) {
        if (visible) {
          response.getWriter().println(JsonFormat.printToString(reply, true));
        } else {
          response.getWriter().println(convertOutput(reply));
        }
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
