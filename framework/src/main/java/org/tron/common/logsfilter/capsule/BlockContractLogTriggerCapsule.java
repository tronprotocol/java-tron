package org.tron.common.logsfilter.capsule;

import static org.tron.common.logsfilter.EventPluginLoader.matchFilter;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.CreateSmartContract;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.pf4j.util.StringUtils;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.ContractEventParserAbi;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.BlockContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

public class BlockContractLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private BlockContractLogTrigger blockContractLogTrigger;


  public BlockContractLogTriggerCapsule(BlockCapsule block, long solidifiedNumber) {
    blockContractLogTrigger = new BlockContractLogTrigger();
    // get block info
    blockContractLogTrigger.setBlockHash(block.getBlockId().toString());
    blockContractLogTrigger.setTimeStamp(block.getTimeStamp());
    blockContractLogTrigger.setBlockNumber(block.getNum());
    blockContractLogTrigger.setLatestSolidifiedBlockNumber(solidifiedNumber);
    // bloom filter
    BloomFilter<String> bloomFilterContractAndTopic = BloomFilter.create(Funnels.stringFunnel(
        Charsets.UTF_8), 80, 0.01);
    BloomFilter<String> bloomFilterContract = BloomFilter.create(Funnels.stringFunnel(
        Charsets.UTF_8), 80, 0.01);
    BloomFilter<String> bloomFilterTopic = BloomFilter.create(Funnels.stringFunnel(
        Charsets.UTF_8), 80, 0.01);

    // get transaction info
    List<BlockContractLogTrigger.TransactionInBlock> transactionInBlockList = new ArrayList<>(block.getTransactions().size());
    blockContractLogTrigger.setTransactionList(transactionInBlockList);
    for (int i = 0; i < block.getTransactions().size(); i++) {
      TransactionCapsule trxCapsule = block.getTransactions().get(i);
      BlockContractLogTrigger.TransactionInBlock transactionInBlock = blockContractLogTrigger.new TransactionInBlock();
      List<ContractTrigger> contractTriggerCapsuleList = trxCapsule.getTrxTrace().getRuntimeResult().getTriggerList();
      if(contractTriggerCapsuleList.isEmpty()){
        continue;
      }
      ByteString contractAddress = ByteString.copyFrom(trxCapsule.getTrxTrace().getRuntimeResult().getContractAddress());
      if (contractAddress.size() > 0) {
          transactionInBlock
              .setContractAddress(StringUtil.encode58Check((contractAddress.toByteArray())));
      }
      transactionInBlock.setTransactionId(trxCapsule.getTransactionId().toString());
      transactionInBlock.setTransactionIndex(i);
      // log trigger
      List<ContractLogTrigger> logTriggers = new ArrayList<>(contractTriggerCapsuleList.size());
      transactionInBlock.setLogList(logTriggers);
      for (ContractTrigger trigger : contractTriggerCapsuleList) {
        // boomFilter
        bloomFilterContract.put(trigger.getContractAddress());
        bloomFilterTopic.put(trigger.getLogInfo().getTopics().get(0).toHexString());
        bloomFilterContractAndTopic.put(
            trigger.getContractAddress()+trigger.getLogInfo().getTopics().get(0).toHexString());

        List<String> filterNames = matchFilter(trigger);
        if (filterNames.isEmpty()) {
          continue;
        }
        trigger.setFilterNameList(filterNames);
        trigger.setBlockHash(blockContractLogTrigger.getBlockHash());
        trigger.setLatestSolidifiedBlockNumber(blockContractLogTrigger.getLatestSolidifiedBlockNumber());
        ContractLogTrigger contractLogTrigger = parseContractTriggerToLogTrigger(trigger);
        logTriggers.add(contractLogTrigger);
      }
      if(!logTriggers.isEmpty()){
        transactionInBlockList.add(transactionInBlock);
      }
    }
    try {
      ByteArrayOutputStream outStreamContract = new ByteArrayOutputStream();
      ByteArrayOutputStream outStreamTopic = new ByteArrayOutputStream();
      ByteArrayOutputStream outStreamContractAndTopic = new ByteArrayOutputStream();
      bloomFilterContract.writeTo(outStreamContract);
      bloomFilterTopic.writeTo(outStreamTopic);
      bloomFilterContractAndTopic.writeTo(outStreamContractAndTopic);
      blockContractLogTrigger.setBloomFilterContract(outStreamContract.toString());
      blockContractLogTrigger.setBloomFilterContract(bloomFilterTopic.toString());
      blockContractLogTrigger.setBloomFilterContract(bloomFilterContractAndTopic.toString());
    } catch (IOException ioE){
    }
  }

  private ContractLogTrigger parseContractTriggerToLogTrigger(ContractTrigger contractTrigger) {
    ContractLogTrigger event = new ContractLogTrigger();
    event.setTriggerName(null);
    LogInfo logInfo = contractTrigger.getLogInfo();
    event.setTopicList(logInfo.getHexTopics());
    event.setData(logInfo.getHexData());
    event.setLogInfo(logInfo);
    RawData rawData = new RawData(logInfo.getAddress(), logInfo.getTopics(), logInfo.getData());
    event.setRawData(rawData);
    event.setLatestSolidifiedBlockNumber(contractTrigger.getLatestSolidifiedBlockNumber());
    event.setRemoved(contractTrigger.isRemoved());
    event.setUniqueId(contractTrigger.getUniqueId());
    event.setTransactionId(contractTrigger.getTransactionId());
    event.setContractAddress(contractTrigger.getContractAddress());
    event.setOriginAddress(contractTrigger.getOriginAddress());
    event.setCallerAddress(contractTrigger.getCallerAddress());
    event.setCreatorAddress(contractTrigger.getCreatorAddress());
    event.setBlockNumber(contractTrigger.getBlockNumber());
    event.setTimeStamp(contractTrigger.getTimeStamp());
    event.setBlockHash(contractTrigger.getBlockHash());
    event.setFilterNameList(contractTrigger.getFilterNameList());
    event.setAbi(contractTrigger.getAbi());
    return event;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postBlockContractLogTrigger(blockContractLogTrigger);
  }
}
