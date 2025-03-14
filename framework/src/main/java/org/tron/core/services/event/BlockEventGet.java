package org.tron.core.services.event;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.ContractEventParserAbi;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.RawData;
import org.tron.common.logsfilter.capsule.SolidityTriggerCapsule;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.BadItemException;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.bo.SmartContractTrigger;
import org.tron.core.store.StoreFactory;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

@Slf4j(topic = "event")
@Component
public class BlockEventGet {

  private EventPluginLoader instance = EventPluginLoader.getInstance();

  @Autowired
  private Manager manager;

  public BlockEvent getBlockEvent(long blockNum) throws Exception {
    BlockCapsule block = manager.getChainBaseManager().getBlockByNum(blockNum);
    block.getTransactions().forEach(t -> t.setBlockNum(block.getNum()));
    long solidNum = manager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    BlockEvent blockEvent = new BlockEvent();
    blockEvent.setBlockId(block.getBlockId());
    blockEvent.setParentId(block.getParentBlockId());
    blockEvent.setSolidId(manager.getChainBaseManager().getBlockIdByNum(solidNum));
    if (instance.isBlockLogTriggerEnable()) {
      blockEvent.setBlockLogTriggerCapsule(getBlockLogTrigger(block, solidNum));
    }

    if (instance.isTransactionLogTriggerEnable()) {
      blockEvent.setTransactionLogTriggerCapsules(getTransactionLogTrigger(block, solidNum));
    }

    if (instance.isContractLogTriggerEnable()
        || instance.isContractEventTriggerEnable()
        || instance.isSolidityLogTriggerEnable()
        || instance.isSolidityEventTriggerEnable()) {
      blockEvent.setSmartContractTrigger(getContractTrigger(block, solidNum));
    }

    if (instance.isSolidityTriggerEnable()) {
      SolidityTriggerCapsule capsule = new SolidityTriggerCapsule(block.getNum());
      capsule.setTimeStamp(block.getTimeStamp());
      blockEvent.setSolidityTriggerCapsule(capsule);
    }

    return blockEvent;
  }

  public SmartContractTrigger getContractTrigger(BlockCapsule block, long solidNum) {
    TransactionRetCapsule result;
    try {
      result = manager.getChainBaseManager().getTransactionRetStore()
        .getTransactionInfoByBlockNum(ByteArray.fromLong(block.getNum()));
    } catch (BadItemException e) {
      throw new RuntimeException(e);
    }

    SmartContractTrigger contractTrigger = new SmartContractTrigger();
    for (int i = 0; i < block.getTransactions().size(); i++) {
      Protocol.Transaction tx = block.getInstance().getTransactions(i);
      Protocol.TransactionInfo txInfo = result.getInstance().getTransactioninfo(i);

      List<ContractTrigger> triggers = parseLogs(tx, txInfo);
      for (ContractTrigger trigger : triggers) {
        if (!EventPluginLoader.matchFilter(trigger)) {
          continue;
        }
        ContractTrigger eventOrLog = processTrigger(trigger);
        eventOrLog.setBlockHash(Hex.toHexString(block.getBlockId().getBytes()));
        eventOrLog.setLatestSolidifiedBlockNumber(solidNum);
        if (eventOrLog instanceof ContractEventTrigger) {
          ContractEventTrigger event = (ContractEventTrigger) eventOrLog;
          if (instance.isContractEventTriggerEnable() || instance.isSolidityEventTriggerEnable()) {
            contractTrigger.getContractEventTriggers().add(event);
          }
          if ((instance.isContractLogTriggerEnable()
              && instance.isContractLogTriggerRedundancy())
              || (instance.isSolidityLogTriggerEnable()
              && instance.isSolidityLogTriggerRedundancy())) {
            ContractLogTrigger logTrigger = new ContractLogTrigger(event);
            logTrigger.setTopicList(trigger.getLogInfo().getHexTopics());
            logTrigger.setData(trigger.getLogInfo().getHexData());
            contractTrigger.getRedundancies().add(logTrigger);
          }
        } else if (eventOrLog instanceof ContractLogTrigger) {
          ContractLogTrigger log = (ContractLogTrigger) eventOrLog;
          if (instance.isContractLogTriggerEnable() || instance.isSolidityLogTriggerEnable()) {
            contractTrigger.getContractLogTriggers().add(log);
          }
        }
      }
    }

    return contractTrigger;
  }

  private List<ContractTrigger> parseLogs(Protocol.Transaction tx,
                                          Protocol.TransactionInfo txInfo) {
    String originAddress = StringUtil
        .encode58Check(TransactionCapsule.getOwner(tx.getRawData().getContract(0)));

    List<Protocol.TransactionInfo.Log> logs = txInfo.getLogList();
    List<ContractTrigger> list = new LinkedList<>();
    if (logs.isEmpty()) {
      return list;
    }

    Map<String, String> addrMap = new HashMap<>();
    Map<String, SmartContractOuterClass.SmartContract.ABI> abiMap = new HashMap<>();
    parseLogs(logs, originAddress, addrMap, abiMap);

    int index = 1;
    for (Protocol.TransactionInfo.Log log : logs) {

      byte[] contractAddress = TransactionTrace
          .convertToTronAddress(log.getAddress().toByteArray());
      String strContractAddress =
          ArrayUtils.isEmpty(contractAddress) ? "" : StringUtil.encode58Check(contractAddress);
      SmartContractOuterClass.SmartContract.ABI abi = abiMap.get(strContractAddress);
      ContractTrigger event = new ContractTrigger();
      String creatorAddr = addrMap.get(strContractAddress);
      String txId = Hex.toHexString(txInfo.getId().toByteArray());
      event.setUniqueId(txId + "_" + index);
      event.setTransactionId(txId);
      event.setContractAddress(strContractAddress);
      event.setOriginAddress(originAddress);
      event.setCallerAddress("");
      event.setCreatorAddress(StringUtils.isEmpty(creatorAddr) ? "" : creatorAddr);
      event.setBlockNumber(txInfo.getBlockNumber());
      event.setTimeStamp(txInfo.getBlockTimeStamp());
      event.setLogInfo(buildLogInfo(log));
      event.setAbi(abi);

      list.add(event);
      index++;
    }

    return list;
  }

  private void parseLogs(List<Protocol.TransactionInfo.Log> logs,
                         String originAddress,
                         Map<String, String> addrMap, Map<String,
                         SmartContractOuterClass.SmartContract.ABI> abiMap) {
    for (Protocol.TransactionInfo.Log log : logs) {

      byte[] contractAddress = TransactionTrace
        .convertToTronAddress(log.getAddress().toByteArray());
      String strContractAddr =
          ArrayUtils.isEmpty(contractAddress) ? "" : StringUtil.encode58Check(contractAddress);
      if (addrMap.get(strContractAddr) != null) {
        continue;
      }
      ContractCapsule contract = manager.getContractStore().get(contractAddress);
      if (contract == null) {
        // never
        addrMap.put(strContractAddr, originAddress);
        abiMap.put(strContractAddr, SmartContractOuterClass.SmartContract.ABI.getDefaultInstance());
        continue;
      }
      AbiCapsule abiCapsule = StoreFactory.getInstance().getChainBaseManager()
          .getAbiStore().get(contractAddress);
      SmartContractOuterClass.SmartContract.ABI abi;
      if (abiCapsule == null || abiCapsule.getInstance() == null) {
        abi = SmartContractOuterClass.SmartContract.ABI.getDefaultInstance();
      } else {
        abi = abiCapsule.getInstance();
      }
      String creatorAddr = StringUtil.encode58Check(TransactionTrace
          .convertToTronAddress(contract.getInstance().getOriginAddress().toByteArray()));
      addrMap.put(strContractAddr, creatorAddr);
      abiMap.put(strContractAddr, abi);
    }
  }

  private LogInfo buildLogInfo(Protocol.TransactionInfo.Log log) {
    List<DataWord> topics = Lists.newArrayList();
    log.getTopicsList().forEach(topic ->
        topics.add(new DataWord(topic.toByteArray()))
    );
    byte[] address = log.getAddress().toByteArray();
    byte[] data = log.getData().toByteArray();
    return new LogInfo(address, topics, data);
  }

  private ContractTrigger processTrigger(ContractTrigger contractTrigger) {
    ContractTrigger event;
    boolean isEvent = false;
    LogInfo logInfo = contractTrigger.getLogInfo();
    SmartContractOuterClass.SmartContract.ABI abi = contractTrigger.getAbi();
    List<DataWord> topics = logInfo.getTopics();

    String eventSignature = "";
    String eventSignatureFull = "fallback()";
    String entryName = "";
    SmartContractOuterClass.SmartContract.ABI.Entry eventEntry = null;

    if (abi != null && abi.getEntrysCount() > 0 && topics != null && !topics.isEmpty()
        && !ArrayUtils.isEmpty(topics.get(0).getData())
        && Args.getInstance().getStorage().isContractParseSwitch()) {
      String logHash = topics.get(0).toString();

      for (SmartContractOuterClass.SmartContract.ABI.Entry entry : abi.getEntrysList()) {
        if (entry.getType() != SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Event
            || entry.getAnonymous()) {
          continue;
        }

        String signature = entry.getName() + "(";
        String signatureFull = entry.getName() + "(";
        StringBuilder signBuilder = new StringBuilder();
        StringBuilder signFullBuilder = new StringBuilder();
        for (SmartContractOuterClass.SmartContract.ABI.Entry.Param param : entry.getInputsList()) {
          if (signBuilder.length() > 0) {
            signBuilder.append(",");
            signFullBuilder.append(",");
          }
          String type = param.getType();
          String name = param.getName();
          signBuilder.append(type);
          signFullBuilder.append(type);
          if (org.pf4j.util.StringUtils.isNotNullOrEmpty(name)) {
            signFullBuilder.append(" ").append(name);
          }
        }
        signature += signBuilder + ")";
        signatureFull += signFullBuilder + ")";
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
    event.setBlockHash(contractTrigger.getBlockHash());

    return event;
  }

  public BlockLogTriggerCapsule getBlockLogTrigger(BlockCapsule block, long solidNum) {
    BlockLogTriggerCapsule blockLogTriggerCapsule = new BlockLogTriggerCapsule(block);
    blockLogTriggerCapsule.setLatestSolidifiedBlockNumber(solidNum);
    return blockLogTriggerCapsule;
  }

  public List<TransactionLogTriggerCapsule> getTransactionLogTrigger(BlockCapsule block,
                                                                     long solidNum) {
    List<TransactionLogTriggerCapsule> transactionLogTriggerCapsules = new ArrayList<>();
    if (!EventPluginLoader.getInstance().isTransactionLogTriggerEthCompatible()) {
      return getTransactionTriggers(block, solidNum);
    }
    List<TransactionCapsule> transactionCapsuleList = block.getTransactions();
    GrpcAPI.TransactionInfoList transactionInfoList = GrpcAPI
        .TransactionInfoList.newBuilder().build();
    GrpcAPI.TransactionInfoList.Builder transactionInfoListBuilder = GrpcAPI
        .TransactionInfoList.newBuilder();
    try {
      TransactionRetCapsule result = manager.getChainBaseManager().getTransactionRetStore()
          .getTransactionInfoByBlockNum(ByteArray.fromLong(block.getNum()));
      if (!Objects.isNull(result) && !Objects.isNull(result.getInstance())) {
        result.getInstance().getTransactioninfoList()
            .forEach(transactionInfoListBuilder::addTransactionInfo);
        transactionInfoList = transactionInfoListBuilder.build();
      }
    } catch (BadItemException e) {
      logger.error("Get TransactionInfo failed, blockNum {}, {}.", block.getNum(), e.getMessage());
    }
    if (transactionCapsuleList.size() != transactionInfoList.getTransactionInfoCount()) {
      logger.error("Get TransactionInfo size not eq, blockNum {}, {}, {}",
          block.getNum(), transactionCapsuleList.size(),
          transactionInfoList.getTransactionInfoCount());
      for (TransactionCapsule t : block.getTransactions()) {
        TransactionLogTriggerCapsule trx = new TransactionLogTriggerCapsule(t, block);
        trx.setLatestSolidifiedBlockNumber(solidNum);
        transactionLogTriggerCapsules.add(trx);
      }
      return transactionLogTriggerCapsules;
    }
    long cumulativeEnergyUsed = 0;
    long cumulativeLogCount = 0;
    long energyUnitPrice = getEnergyPrice(block.getTimeStamp());
    for (int i = 0; i < transactionCapsuleList.size(); i++) {
      Protocol.TransactionInfo transactionInfo = transactionInfoList.getTransactionInfo(i);
      TransactionCapsule transactionCapsule = transactionCapsuleList.get(i);
      transactionCapsule.setBlockNum(block.getNum());
      TransactionLogTriggerCapsule trx = new TransactionLogTriggerCapsule(transactionCapsule, block,
          i, cumulativeEnergyUsed, cumulativeLogCount, transactionInfo, energyUnitPrice, true);
      trx.setLatestSolidifiedBlockNumber(solidNum);
      cumulativeEnergyUsed += trx.getTransactionLogTrigger().getEnergyUsageTotal();
      cumulativeLogCount += transactionInfo.getLogCount();
      transactionLogTriggerCapsules.add(trx);
    }
    return transactionLogTriggerCapsules;
  }

  public long getEnergyPrice(long blockTime) {
    String energyPriceHistory = manager.getDynamicPropertiesStore().getEnergyPriceHistory();

    String[] energyPrices = energyPriceHistory.split(",");
    String[] lastPrice = energyPrices[energyPrices.length - 1].split(":");
    long energyPrice = Long.parseLong(lastPrice[1]);

    for (int i = 1; i < energyPrices.length; i++) {
      long effectiveTime = Long.parseLong(energyPrices[i].split(":")[0]);
      if (blockTime < effectiveTime) {
        energyPrice = Long.parseLong(energyPrices[i - 1].split(":")[1]);
        break;
      }
    }
    return energyPrice;
  }

  public List<TransactionLogTriggerCapsule> getTransactionTriggers(BlockCapsule block,
                                                                   long solidNum) {
    List<TransactionLogTriggerCapsule> list = new ArrayList<>();
    if (block.getTransactions().size() == 0) {
      return list;
    }

    GrpcAPI.TransactionInfoList transactionInfoList = GrpcAPI
        .TransactionInfoList.newBuilder().build();
    GrpcAPI.TransactionInfoList.Builder transactionInfoListBuilder = GrpcAPI
        .TransactionInfoList.newBuilder();
    try {
      TransactionRetCapsule result = manager.getChainBaseManager().getTransactionRetStore()
          .getTransactionInfoByBlockNum(ByteArray.fromLong(block.getNum()));
      if (!Objects.isNull(result) && !Objects.isNull(result.getInstance())) {
        result.getInstance().getTransactioninfoList()
            .forEach(transactionInfoListBuilder::addTransactionInfo);
        transactionInfoList = transactionInfoListBuilder.build();
      }
    } catch (Exception e) {
      logger.warn("Get TransactionInfo failed, blockNum {}, {}.", block.getNum(), e.getMessage());
    }

    if (block.getTransactions().size() != transactionInfoList.getTransactionInfoCount()) {
      for (TransactionCapsule t : block.getTransactions()) {
        TransactionLogTriggerCapsule triggerCapsule = new TransactionLogTriggerCapsule(t, block);
        triggerCapsule.setLatestSolidifiedBlockNumber(solidNum);
        list.add(triggerCapsule);
      }
    } else {
      for (int i = 0; i < transactionInfoList.getTransactionInfoCount(); i++) {
        TransactionLogTriggerCapsule triggerCapsule = new TransactionLogTriggerCapsule(
            block.getTransactions().get(i), block, transactionInfoList.getTransactionInfo(i));
        triggerCapsule.setLatestSolidifiedBlockNumber(solidNum);
        list.add(triggerCapsule);
      }
    }

    return list;
  }
}