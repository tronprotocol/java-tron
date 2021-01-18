package org.tron.core.services.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.IvkDecryptTRC20Parameters;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class ScanShieldedTRC20NotesByIvkServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  public static String convertOutput(GrpcAPI.DecryptNotesTRC20 notes, boolean visible) {
    String resultString = JsonFormat.printToString(notes, visible);
    if (notes.getNoteTxsCount() == 0) {
      return resultString;
    } else {
      JSONObject jsonNotes = JSONObject.parseObject(resultString);
      JSONArray array = jsonNotes.getJSONArray("noteTxs");
      for (int index = 0; index < array.size(); index++) {
        JSONObject item = array.getJSONObject(index);
        item.put("index", notes.getNoteTxs(index).getIndex()); // Avoid automatically ignoring 0
      }
      return jsonNotes.toJSONString();
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      IvkDecryptTRC20Parameters.Builder ivkDecryptTRC20Parameters = IvkDecryptTRC20Parameters
          .newBuilder();
      JsonFormat.merge(params.getParams(), ivkDecryptTRC20Parameters, params.isVisible());

      GrpcAPI.DecryptNotesTRC20 notes = wallet
          .scanShieldedTRC20NotesByIvk(ivkDecryptTRC20Parameters.getStartBlockIndex(),
              ivkDecryptTRC20Parameters.getEndBlockIndex(),
              ivkDecryptTRC20Parameters.getShieldedTRC20ContractAddress().toByteArray(),
              ivkDecryptTRC20Parameters.getIvk().toByteArray(),
              ivkDecryptTRC20Parameters.getAk().toByteArray(),
              ivkDecryptTRC20Parameters.getNk().toByteArray(),
              ivkDecryptTRC20Parameters.getEventsList());
      response.getWriter().println(convertOutput(notes, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long startNum = Long.parseLong(request.getParameter("start_block_index"));
      long endNum = Long.parseLong(request.getParameter("end_block_index"));
      String ivk = request.getParameter("ivk");

      String contractAddress = request.getParameter("shielded_TRC20_contract_address");
      if (visible) {
        contractAddress = Util.getHexAddress(contractAddress);
      }

      String ak = request.getParameter("ak");
      String nk = request.getParameter("nk");

      GrpcAPI.DecryptNotesTRC20 notes = wallet
          .scanShieldedTRC20NotesByIvk(startNum, endNum,
              ByteArray.fromHexString(contractAddress), ByteArray.fromHexString(ivk),
              ByteArray.fromHexString(ak), ByteArray.fromHexString(nk), null);
      response.getWriter().println(convertOutput(notes, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
