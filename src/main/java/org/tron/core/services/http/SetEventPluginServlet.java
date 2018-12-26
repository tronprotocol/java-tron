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
public class SetEventPluginServlet extends HttpServlet {
    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        System.out.println("SetEventPluginServlet doPost");


        GrpcAPI.EventPluginInfo.Builder pluginConfigBuilder = GrpcAPI.EventPluginInfo.newBuilder();
        GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();

        String jsonValue = "";

        try {
            jsonValue = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(jsonValue);

        try {
            JsonFormat.merge(jsonValue, pluginConfigBuilder);
        } catch (JsonFormat.ParseException e) {
            e.printStackTrace();
        }

        if (Objects.isNull(pluginConfigBuilder)){
            return;
        }

        wallet.setEventPluginInfo(pluginConfigBuilder);

        // todo

    }
}
