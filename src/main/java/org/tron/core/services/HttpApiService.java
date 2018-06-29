package org.tron.core.services;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.tron.core.services.http.AccountServlet;
import org.tron.core.services.http.BlockServlet;


public class HttpApiService {

  public static void main(String[] args) throws Exception {


    Server server = new Server(8006);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);

    context.addServlet(new ServletHolder(new AccountServlet()), "/account");
    context.addServlet(new ServletHolder(new BlockServlet()), "/block");

    server.start();
    server.join();
  }
}
