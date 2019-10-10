package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.ContractEventParserAbi;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.SmartContract.ABI;

import java.util.List;

public class ContractTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private ContractTrigger contractTrigger;

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
    LogInfo logInfo = contractTrigger.getLogInfo();
    ABI abi = contractTrigger.getAbi();
    List<DataWord> topics = logInfo.getTopics();

    String eventSignature = "";
    String eventSignatureFull = "fallback()";
    String entryName = "";
    ABI.Entry eventEntry = null;

    if (abi != null && abi.getEntrysCount() > 0 && topics != null && !topics.isEmpty()
        && !ArrayUtils.isEmpty(topics.get(0).getData()) && Args.getInstance().getStorage()
        .isContractParseSwitch()) {
      String logHash = topics.get(0).toString();

      for (ABI.Entry entry : abi.getEntrysList()) {
        if (entry.getType() != ABI.Entry.EntryType.Event || entry.getAnonymous()) {
          continue;
        }

        String signature = entry.getName() + "(";
        String signatureFull = entry.getName() + "(";
        StringBuilder signBuilder = new StringBuilder();
        StringBuilder signFullBuilder = new StringBuilder();
        for (ABI.Entry.Param param : entry.getInputsList()) {
          if (signBuilder.length() > 0) {
            signBuilder.append(",");
            signFullBuilder.append(",");
          }
          String type = param.getType();
          String name = param.getName();
          signBuilder.append(type);
          signFullBuilder.append(type);
          if (StringUtils.isNotNullOrEmpty(name)) {
            signFullBuilder.append(" ").append(name);
          }
        }
        signature += signBuilder.toString() + ")";
        signatureFull += signFullBuilder.toString() + ")";
        String sha3 = Hex.toHexString(Hash.sha3(signature.getBytes()));
        if (sha3.equals(logHash)) {
          eventSignature = signature;
          eventSignatureFull = signatureFull;
          entryName = entry.getName();
          eventEntry = entry;
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
          .setTopicMap(ContractEventParserAbi.parseTopics(topicList, eventEntry));
      ((ContractEventTrigger) event)
          .setDataMap(ContractEventParserAbi.parseEventData(data, topicList, eventEntry));
    } else {
      if (!EventPluginLoader.getInstance().isContractLogTriggerEnable()) {
        return;
      }
      event = new ContractLogTrigger();
      ((ContractLogTrigger) event).setTopicList(logInfo.getHexTopics());
      ((ContractLogTrigger) event).setData(logInfo.getHexData());
    }

    RawData rawData = new RawData(logInfo.getAddress(), logInfo.getTopics(), logInfo.getData());

    event.setRawData(rawData);

    event.setLatestSolidifiedBlockNumber(contractTrigger.getLatestSolidifiedBlockNumber());
    event.setRemoved(contractTrigger.isRemoved());
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
