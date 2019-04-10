package org.tron.common.logsfilter.capsule;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.LogInfo;

public class ContractTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  ContractTrigger contractTrigger;

  public ContractTriggerCapsule(ContractTrigger contractTrigger) {
    this.contractTrigger = contractTrigger;
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    contractTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  @Override
  public void processTrigger() {
    ContractTrigger event;
    boolean isEvent = false;
    LogInfo logInfo = contractTrigger.getRawData();
    JSONObject abi = JSONObject.parseObject(contractTrigger.getAbiString());
    JSONArray entrys = abi.getJSONArray("entrys");
    String eventSignature = "";
    String eventSignatureFull = "fallback()";
    String entryName = "";

    if (entrys != null) {
      for (int i = 0; i < entrys.size(); i++) {
        JSONObject entry = entrys.getJSONObject(i);

        if (!entry.getString("type").equalsIgnoreCase("event") || entry
            .getBoolean("anonymous")) {
          continue;
        }

        String signature = entry.getString("name") + "(";
        String signatureFull = entry.getString("name") + "(";
        StringBuilder signBuilder = new StringBuilder();
        StringBuilder signFullBuilder = new StringBuilder();
        JSONArray inputs = entry.getJSONArray("inputs");
        if (inputs != null) {
          for (int j = 0; j < inputs.size(); j++) {
            if (signBuilder.length() > 0) {
              signBuilder.append(",");
              signFullBuilder.append(",");
            }
            signBuilder.append(inputs.getJSONObject(j).getString("type"));
            signFullBuilder.append(" ").append(inputs.getJSONObject(j).getString("name"));
          }
        }
        signature += signBuilder.toString() + ")";
        signatureFull += signFullBuilder.toString() + ")";
        String sha3 = Hex.toHexString(Hash.sha3(signature.getBytes()));
        if (sha3.equals(logInfo.getTopics().get(0).toString())) {
          eventSignature = signature;
          eventSignatureFull = signatureFull;
          entryName = entry.getString("name");
          isEvent = true;
          break;
        }
      }
    }

    if (isEvent) {
      event = new ContractEventTrigger();
      ((ContractEventTrigger) event).setEventSignature(eventSignature);
      ((ContractEventTrigger) event).setEventSignatureFull(eventSignatureFull);
      ((ContractEventTrigger) event).setEventName(entryName);

      List<byte[]> topicList = logInfo.getClonedTopics();
      byte[] data = logInfo.getClonedData();

//      this.abiEntry = log.getAbiEntry();
//
//      ((ContractEventTrigger) event)
//          .setTopicMap(ContractEventParser.parseTopics(topicList, abiEntry));
//      ((ContractEventTrigger) event)
//          .setDataMap(ContractEventParser.parseEventData(data, topicList, abiEntry));

    } else {
      event = new ContractLogTrigger();
      ((ContractLogTrigger) event).setTopicList(logInfo.getHexTopics());
      ((ContractLogTrigger) event).setData(logInfo.getHexData());
    }

    event.setUniqueId(contractTrigger.getUniqueId());
    event.setTransactionId(contractTrigger.getTransactionId());
    event.setContractAddress(contractTrigger.getContractAddress());
    event.setOriginAddress(contractTrigger.getOriginAddress());
    event.setCallerAddress("");
    event.setCreatorAddress(contractTrigger.getCreatorAddress());
    event.setBlockNumber(contractTrigger.getBlockNumber());
    event.setTimeStamp(contractTrigger.getTimeStamp());

    if (FilterQuery.matchFilter(contractTrigger)) {
      if (isEvent) {
        EventPluginLoader.getInstance().postContractEventTrigger((ContractEventTrigger) event);
      } else {
        EventPluginLoader.getInstance().postContractLogTrigger((ContractLogTrigger) event);
      }
    }
  }
}
