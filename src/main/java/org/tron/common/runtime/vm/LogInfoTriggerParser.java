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
                                                    BlockCapsule block,
                                                    List<LogInfo> logInfos,
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
    for (ABI.Entry entry: abi.getEntrysList()) {
      if (entry.getType() != ABI.Entry.EntryType.Event) {
        continue;
      }
      String signature = entry.getName() + "(";
      StringBuilder builder = new StringBuilder();
      for (ABI.Entry.Param param: entry.getInputsList()) {
        if (builder.length() > 0){
          builder.append(",");
        }
        builder.append(param.getType());
      }
      signature += builder.toString() + ")";
      byte[] sha3 = Hash.sha3(signature.getBytes());
      fullMap.put(sha3, entry);
      signMap.put(sha3, signature);
    }

    Long blockNum = block.getNum();
    Long blockTimestamp = block.getTimeStamp();
    String txIdStr = ArrayUtils.isEmpty(txId) ? null : Hex.toHexString(txId);
    String callerAddrStr = ArrayUtils.isEmpty(callerAddress) ? null : Wallet.encode58Check(callerAddress);
    String contractAddrStr = ArrayUtils.isEmpty(contractAddress) ? null : Wallet.encode58Check(contractAddress);
    String originAddrStr = ArrayUtils.isEmpty(originAddress) ? null :Wallet.encode58Check(originAddress);
    String creatorAddrStr = ArrayUtils.isEmpty(creatorAddress) ? null : Wallet.encode58Check(creatorAddress);


    for (LogInfo logInfo: logInfos) {
      List<DataWord> topics = logInfo.getTopics();
      if (topics.size() > 0 && fullMap.size() > 0){
        byte[] firstTopic = logInfo.getTopics().get(0).getData();
        ABI.Entry entry = fullMap.get(firstTopic);

        ContractTrigger event;
        if (entry == null){
          event = new ContractLogTrigger(txIdStr, contractAddrStr, callerAddrStr, originAddrStr, creatorAddrStr, blockNum, blockTimestamp);
          ((ContractLogTrigger) event).setTopicList(logInfo.getHexTopics());
          ((ContractLogTrigger) event).setData(Hex.toHexString(logInfo.getData()));
        }else {
          event = new ContractEventTrigger(txIdStr, contractAddrStr, callerAddrStr, originAddrStr, creatorAddrStr, blockNum, blockTimestamp);
          ((ContractEventTrigger) event).setEventSignature(signMap.get(firstTopic));
          ((ContractEventTrigger) event).setTopicMap(parseEventTopics(logInfo.getTopics(), entry.getInputsList()));
          ((ContractEventTrigger) event).setDataMap(parseEventData(logInfo.getData(), entry.getInputsList()));
        }
      }

    }
    return list;
  }

  private static Map<String, Object> parseEventTopics(List<DataWord> topicList, List< ABI.Entry.Param > abiInputList) {
    return null;
  }

  private static Map<String, Object> parseEventData(byte[] data, List< ABI.Entry.Param > abiInputList) {
    return null;
  }
}
