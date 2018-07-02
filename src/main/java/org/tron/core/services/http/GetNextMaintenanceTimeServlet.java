package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.core.Wallet;

@Component
@Slf4j
public class GetNextMaintenanceTimeServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    NumberMessage reply = wallet.getNextMaintenanceTime();
    response.getWriter().println(JsonFormat.printToString(reply));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}