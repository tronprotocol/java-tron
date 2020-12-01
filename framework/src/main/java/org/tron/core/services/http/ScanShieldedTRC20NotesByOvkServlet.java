package org.tron.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.OvkDecryptTRC20Parameters;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class ScanShieldedTRC20NotesByOvkServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      OvkDecryptTRC20Parameters.Builder ovkDecryptTRC20Parameters = OvkDecryptTRC20Parameters
          .newBuilder();
      JsonFormat.merge(params.getParams(), ovkDecryptTRC20Parameters, params.isVisible());

      GrpcAPI.DecryptNotesTRC20 notes = wallet
          .scanShieldedTRC20NotesByOvk(ovkDecryptTRC20Parameters.getStartBlockIndex(),
              ovkDecryptTRC20Parameters.getEndBlockIndex(),
              ovkDecryptTRC20Parameters.getOvk().toByteArray(),
              ovkDecryptTRC20Parameters.getShieldedTRC20ContractAddress().toByteArray(),
              ovkDecryptTRC20Parameters.getEventsList()
          );
      response.getWriter()
          .println(ScanShieldedTRC20NotesByIvkServlet.convertOutput(notes, params.isVisible()));
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
      String contractAddress = request.getParameter("shielded_TRC20_contract_address");
      if (visible) {
        contractAddress = Util.getHexAddress(contractAddress);
      }
      GrpcAPI.DecryptNotesTRC20 notes = wallet
          .scanShieldedTRC20NotesByOvk(startBlockIndex, endBlockIndex,
              ByteArray.fromHexString(ovk), ByteArray.fromHexString(contractAddress), null);

      response.getWriter()
          .println(ScanShieldedTRC20NotesByIvkServlet.convertOutput(notes, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
