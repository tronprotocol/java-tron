package org.tron.core.services.http;


import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.Builder;


@Component
public class AccountServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String account = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Builder build = Account.newBuilder();
    JsonFormat.merge(account, build);

    response.getWriter().println("account");
    response.getWriter().println(Wallet.encode58Check(build.build().getAddress().toByteArray()));

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
