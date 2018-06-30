package org.tron.core.services.http;


import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
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
import org.tron.protos.Protocol.Account.Builder;


@Component
@Slf4j
public class GetAccountServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  @Autowired
  private Manager dbManager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String account = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Builder build = Account.newBuilder();
    JsonFormat.merge(account, build);
    Account reply = wallet.getAccount(build.build());
    AccountCapsule accountCapsule = new AccountCapsule(reply);
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);
    response.getWriter().println(JsonFormat.printToString(accountCapsule.getInstance()));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}
