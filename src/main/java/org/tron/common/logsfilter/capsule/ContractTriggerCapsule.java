package org.tron.common.logsfilter.capsule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.ContractEventParserJson;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.config.args.Args;

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
    JSONObject abi = null;
    JSONArray entrys = null;
    String abiString = contractTrigger.getAbiString();

    Object abiObj = JSON.parse(abiString);
    if (abiObj instanceof JSONObject) {
      abi = (JSONObject) abiObj;
      entrys = abi.getJSONArray("entrys");
    }

    List<DataWord> topics = logInfo.getTopics();

    String eventSignature = "";
    String eventSignatureFull = "fallback()";
    String entryName = "";
    JSONObject entryObj = new JSONObject();

    if (entrys != null && topics != null && !topics.isEmpty() && !ArrayUtils
        .isEmpty(topics.get(0).getData()) && Args.getInstance().getStorage()
        .isContractParseSwitch()) {
      String logHash = topics.get(0).toString();
      for (int i = 0; i < entrys.size(); i++) {
        JSONObject entry = entrys.getJSONObject(i);

        String funcType = entry.getString("type");
        Boolean anonymous = entry.getBoolean("anonymous");
        if (funcType == null || !funcType.equalsIgnoreCase("event")) {
          continue;
        }

        if (anonymous != null && anonymous) {
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
            String type = inputs.getJSONObject(j).getString("type");
            String name = inputs.getJSONObject(j).getString("name");
            signBuilder.append(type);
            signFullBuilder.append(type);
            if (StringUtils.isNotNullOrEmpty(name)) {
              signFullBuilder.append(" ").append(name);
            }
          }
        }
        signature += signBuilder.toString() + ")";
        signatureFull += signFullBuilder.toString() + ")";
        String sha3 = Hex.toHexString(Hash.sha3(signature.getBytes()));
        if (sha3.equals(logHash)) {
          eventSignature = signature;
          eventSignatureFull = signatureFull;
          entryName = entry.getString("name");
          entryObj = entry;
          isEvent = true;
          break;
        }
      }
    }

    if (isEvent) {
      if (!EventPluginLoader.getInstance().isContractEventTriggerEnable()) {
        return;
      }
      event = new ContractEventTrigger();
      ((ContractEventTrigger) event).setEventSignature(eventSignature);
      ((ContractEventTrigger) event).setEventSignatureFull(eventSignatureFull);
      ((ContractEventTrigger) event).setEventName(entryName);

      List<byte[]> topicList = logInfo.getClonedTopics();
      byte[] data = logInfo.getClonedData();

      ((ContractEventTrigger) event)
          .setTopicMap(ContractEventParserJson.parseTopics(topicList, entryObj));
      ((ContractEventTrigger) event)
          .setDataMap(ContractEventParserJson.parseEventData(data, topicList, entryObj));
    } else {
      if (!EventPluginLoader.getInstance().isContractLogTriggerEnable()) {
        return;
      }
      event = new ContractLogTrigger();
      ((ContractLogTrigger) event).setTopicList(logInfo.getHexTopics());
      ((ContractLogTrigger) event).setData(logInfo.getHexData());
    }

    event.setRawData(logInfo);
    event.setAbiString(contractTrigger.getAbiString());

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
