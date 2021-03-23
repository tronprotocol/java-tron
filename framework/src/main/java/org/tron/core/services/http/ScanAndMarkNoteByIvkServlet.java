package org.tron.core.services.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.IvkDecryptAndMarkParameters;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class ScanAndMarkNoteByIvkServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  /*
  add some column of default value
   */
  private String convertOutput(GrpcAPI.DecryptNotesMarked notes, boolean visible) {
    String resultString = JsonFormat.printToString(notes, visible);
    if (notes.getNoteTxsCount() == 0) {
      return resultString;
    } else {
      JSONObject markedNotes = JSONObject.parseObject(resultString);
      JSONArray array = markedNotes.getJSONArray("noteTxs");
      for (int index = 0; index < array.size(); index++) {
        JSONObject item = array.getJSONObject(index);
        item.put("is_spend", notes.getNoteTxs(index).getIsSpend());
        item.put("index", notes.getNoteTxs(index).getIndex());
      }
      return markedNotes.toJSONString();
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);

      IvkDecryptAndMarkParameters.Builder ivkDecryptParameters =
          IvkDecryptAndMarkParameters.newBuilder();
      JsonFormat.merge(params.getParams(), ivkDecryptParameters);

      GrpcAPI.DecryptNotesMarked notes = wallet
          .scanAndMarkNoteByIvk(ivkDecryptParameters.getStartBlockIndex(),
              ivkDecryptParameters.getEndBlockIndex(),
              ivkDecryptParameters.getIvk().toByteArray(),
              ivkDecryptParameters.getAk().toByteArray(),
              ivkDecryptParameters.getNk().toByteArray());

      response.getWriter().println(convertOutput(notes, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long startNum = Long.parseLong(request.getParameter("start_block_index"));
      long endNum = Long.parseLong(request.getParameter("end_block_index"));
      String ivk = request.getParameter("ivk");
      String ak = request.getParameter("ak");
      String nk = request.getParameter("nk");
      boolean visible = Util.getVisible(request);

      GrpcAPI.DecryptNotesMarked notes = wallet
          .scanAndMarkNoteByIvk(startNum, endNum, ByteArray.fromHexString(ivk),
              ByteArray.fromHexString(ak), ByteArray.fromHexString(nk));

      response.getWriter().println(convertOutput(notes, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
