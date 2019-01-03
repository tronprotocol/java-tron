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
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SetEventPluginConfigServlet extends HttpServlet {
    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        GrpcAPI.EventPluginInfo.Builder pluginConfigBuilder = GrpcAPI.EventPluginInfo.newBuilder();
        String jsonValue = "";

        try {
            jsonValue = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            logger.error("{}", e);
        }

        try {
            JsonFormat.merge(jsonValue, pluginConfigBuilder);
        } catch (JsonFormat.ParseException e) {
            logger.error("{}", e);
        }

        if (Objects.isNull(pluginConfigBuilder) || pluginConfigBuilder.getTriggerInfoList().size() == 0){
            try {
                response.getWriter().println("failed");
            } catch (IOException e) {
                logger.error("{}", e);
            }
            return;
        }

        GrpcAPI.Return ret = wallet.setEventPluginInfo(pluginConfigBuilder);

        try {
            if (ret.getResult()){
                response.getWriter().println("ok");
            }
            else {
                response.getWriter().println("failed");
            }

        } catch (IOException e) {
            logger.error("{}", e);
        }
    }
}
