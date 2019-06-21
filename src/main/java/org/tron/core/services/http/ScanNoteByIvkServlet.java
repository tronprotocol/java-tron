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
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.IvkDecryptParameters;
import org.tron.api.GrpcAPI.OvkDecryptParameters;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class ScanNoteByIvkServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
              .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      IvkDecryptParameters.Builder ivkDecryptParameters = IvkDecryptParameters.newBuilder();
      JsonFormat.merge(input, ivkDecryptParameters);
      
      GrpcAPI.DecryptNotes notes = wallet
          .scanNoteByIvk(ivkDecryptParameters.getStartBlockIndex(),
                  ivkDecryptParameters.getEndBlockIndex(),
                  ivkDecryptParameters.getIvk().toByteArray());

      response.getWriter()
          .println(JsonFormat.printToString(notes, visible));
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
    try {
      long startNum = Long.parseLong(request.getParameter("start_block_index"));
      long endNum = Long.parseLong(request.getParameter("end_block_index"));
      String ivk = request.getParameter("ivk");
      boolean visible = Util.getVisible(request);

      GrpcAPI.DecryptNotes notes = wallet
          .scanNoteByIvk(startNum, endNum, ByteArray.fromHexString(ivk));
      response.getWriter()
          .println(JsonFormat.printToString(notes, visible));
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
