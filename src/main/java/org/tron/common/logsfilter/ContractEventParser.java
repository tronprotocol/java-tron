package org.tron.common.logsfilter;

import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.SmartContract.ABI;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContractEventParser {

  private static Map<String, Object> parseTopics(ContractEventTrigger trigger) {
    List<DataWord> topicList = trigger.getTopicList();
    ABI.Entry entry = trigger.getAbiEntry();

    List< ABI.Entry.Param > inputList = entry.getInputsList();
    int index = 0;
    Map<String, Object> map = new HashMap<>();
    for (ABI.Entry.Param param : inputList) {
      if (!param.getIndexed()){
        continue;
      }
      if (index >= topicList.size()){
        break;
      }
      map.put(param.getName(), topicList.get(index++));
    }
    return null;
  }

  private static Map<String, Object> parseEventData(ContractEventTrigger trigger) {

    return null;
  }
}
