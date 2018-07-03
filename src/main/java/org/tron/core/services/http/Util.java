package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;


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

  public static String printTransaction(Transaction transaction) {
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(transaction));
    String txID = ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    jsonObject.put("txID", txID);
    return jsonObject.toJSONString();
  }
}
