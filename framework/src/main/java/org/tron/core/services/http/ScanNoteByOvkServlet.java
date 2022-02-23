package org.tron.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.OvkDecryptParameters;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class ScanNoteByOvkServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);

      OvkDecryptParameters.Builder ovkDecryptParameters = OvkDecryptParameters.newBuilder();
      JsonFormat.merge(params.getParams(), ovkDecryptParameters);

      GrpcAPI.DecryptNotes notes = wallet
          .scanNoteByOvk(ovkDecryptParameters.getStartBlockIndex(),
              ovkDecryptParameters.getEndBlockIndex(),
              ovkDecryptParameters.getOvk().toByteArray());
      response.getWriter().println(ScanNoteByIvkServlet.convertOutput(notes, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long startBlockIndex = Long.parseLong(request.getParameter("start_block_index"));
      long endBlockIndex = Long.parseLong(request.getParameter("end_block_index"));
      String ovk = request.getParameter("ovk");
      GrpcAPI.DecryptNotes notes = wallet
          .scanNoteByOvk(startBlockIndex, endBlockIndex, ByteArray.fromHexString(ovk));
      response.getWriter().println(ScanNoteByIvkServlet.convertOutput(notes, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
