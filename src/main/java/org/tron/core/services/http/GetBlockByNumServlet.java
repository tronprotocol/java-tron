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
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Protocol.Block;

@Component
@Slf4j
public class GetBlockByNumServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(input, build);
      Block reply = wallet.getBlockByNum(build.getNum());
      if (reply != null) {
        BlockCapsule blockCapsule = new BlockCapsule(reply);
        String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
        JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(reply));
        jsonObject.put("blockID", blockID);
        response.getWriter().println(jsonObject);
      } else {
        response.getWriter().println("{}");
      }
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}