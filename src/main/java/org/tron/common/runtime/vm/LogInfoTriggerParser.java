package org.tron.common.runtime.vm;

import com.cedarsoftware.util.ByteUtilities;
import com.googlecode.cqengine.query.simple.Has;
import org.abego.treelayout.internal.util.java.util.ListUtil;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.SmartContract.ABI;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LogInfoTriggerParser {

  public static List<ContractTrigger> parseLogInfos(ABI abi,
                                                    List<LogInfo> logInfos,
                                                    Long blockNum,
                                                    Long blockTimestamp,
                                                    byte[] txId,
                                                    byte[] callerAddress,
                                                    byte[] creatorAddress,
                                                    byte[] originAddress,
                                                    byte[] contractAddress) {

    List<ContractTrigger> list = new LinkedList<>();
    if (logInfos == null || logInfos.size() <= 0){
      return list;
    }

    Map<byte[], ABI.Entry> fullMap = new HashMap<>();
    Map<byte[], String> signMap = new HashMap<>();

    // calculate the sha3 of the event signature first.
    if (abi != null && abi.getEntrysCount() > 0){
      for (ABI.Entry entry: abi.getEntrysList()) {
        if (entry.getType() != ABI.Entry.EntryType.Event || entry.getAnonymous()) {
          continue;
        }
        String signature = getEntrySignature(entry);
        byte[] sha3 = Hash.sha3(signature.getBytes());
        fullMap.put(sha3, entry);
        signMap.put(sha3, signature);
      }
    }

    String txIdStr = ArrayUtils.isEmpty(txId) ? "" : Hex.toHexString(txId);
    String callerAddrStr = ArrayUtils.isEmpty(callerAddress) ? "" : Wallet.encode58Check(callerAddress);
    String contractAddrStr = ArrayUtils.isEmpty(contractAddress) ? "" : Wallet.encode58Check(contractAddress);
    String originAddrStr = ArrayUtils.isEmpty(originAddress) ? "" :Wallet.encode58Check(originAddress);
    String creatorAddrStr = ArrayUtils.isEmpty(creatorAddress) ? "" : Wallet.encode58Check(creatorAddress);

    for (LogInfo logInfo: logInfos) {
      List<DataWord> topics = logInfo.getTopics();
      ABI.Entry entry = null;
      String signature = "";
      if (topics != null && topics.size() > 0 && !ArrayUtils.isEmpty(topics.get(0).getData()) && fullMap.size() > 0) {
        byte[] firstTopic = topics.get(0).getData();
        entry = fullMap.get(firstTopic);
        signature = signMap.get(firstTopic);
      }

      boolean isEvent = (entry != null);
      ContractLogTrigger event = isEvent ?
          new LogEventWrapper(txIdStr, contractAddrStr, callerAddrStr, originAddrStr, creatorAddrStr, blockNum, blockTimestamp) :
          new ContractLogTrigger(txIdStr, contractAddrStr, callerAddrStr, originAddrStr, creatorAddrStr, blockNum, blockTimestamp);

      event.setTopicList(logInfo.getClonedTopics());
      event.setData(logInfo.getClonedData());

      if (isEvent){
        ((LogEventWrapper) event).setEventSignature(signature);
        ((LogEventWrapper) event).setAbiEntry(entry);
      }
      list.add(event);
    }
    return list;
  }

  private static String getEntrySignature(ABI.Entry entry) {
    String signature = entry.getName() + "(";
    StringBuilder builder = new StringBuilder();
    for (ABI.Entry.Param param: entry.getInputsList()) {
      if (builder.length() > 0){
        builder.append(",");
      }
      builder.append(param.getType());
    }
    signature += builder.toString() + ")";
    return signature;
  }
}
