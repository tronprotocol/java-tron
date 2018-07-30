package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Account;


@Component
@Slf4j
public class GetAccountServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  @Autowired
  private Manager dbManager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String address = request.getParameter("address");
      Account.Builder build = Account.newBuilder();
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("address", address);
      JsonFormat.merge(jsonObject.toJSONString(), build);
      Account reply = wallet.getAccount(build.build());
      if (reply != null) {
        AccountCapsule accountCapsule = new AccountCapsule(reply);
        BandwidthProcessor processor = new BandwidthProcessor(dbManager);
        processor.updateUsage(accountCapsule);
        response.getWriter().println(JsonFormat.printToString(accountCapsule.getInstance()));
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
      Account.Builder build = Account.newBuilder();
      JsonFormat.merge(account, build);
      Account reply = wallet.getAccount(build.build());
      if (reply != null) {
        AccountCapsule accountCapsule = new AccountCapsule(reply);
        BandwidthProcessor processor = new BandwidthProcessor(dbManager);
        processor.updateUsage(accountCapsule);
        response.getWriter().println(JsonFormat.printToString(accountCapsule.getInstance()));
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
