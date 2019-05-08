package org.tron.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j(topic = "API")
public class ScanNoteByBlockrangeAndIvkServlet extends HttpServlet{
    @Autowired
    private Wallet wallet;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            long startNum = Long.parseLong(request.getParameter("startNum"));
            long endNum = Long.parseLong(request.getParameter("endNum"));
            String ivk = request.getParameter("ivk");

            GrpcAPI.DecryptNotes notes = wallet.scanNoteByBlockRangeAndIvk(startNum,endNum,ByteArray.fromHexString(ivk));

            response.getWriter()
                    .println(JsonFormat.printToString(notes));

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
