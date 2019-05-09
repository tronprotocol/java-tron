package org.tron.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Slf4j(topic = "API")
public class GetDiversifierServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            GrpcAPI.DiversifierMessage d = wallet.getDiversifier();

            if (d != null) {
                response.getWriter().println(JsonFormat.printToString(d));
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
       doPost(request, response);
    }
}
