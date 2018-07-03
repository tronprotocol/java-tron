package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;


public class Util {

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getMessage());
    return jsonObject.toJSONString();
  }

  public static String printBlock(Block block) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(block));
    jsonObject.put("blockID", blockID);
    return jsonObject.toJSONString();
  }
}
