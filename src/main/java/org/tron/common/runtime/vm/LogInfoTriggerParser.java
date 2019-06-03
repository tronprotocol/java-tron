package org.tron.common.runtime.vm;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.utils.MUtil;
import org.tron.common.storage.Deposit;
import org.tron.core.Wallet;
import org.tron.core.capsule.ContractCapsule;
import org.tron.protos.Protocol.SmartContract.ABI;

@Slf4j
public class LogInfoTriggerParser {

  private Long blockNum;
  private Long blockTimestamp;
  private String txId;
  private String originAddress;

  public LogInfoTriggerParser(Long blockNum,
      Long blockTimestamp,
      byte[] txId, byte[] originAddress) {

    this.blockNum = blockNum;
    this.blockTimestamp = blockTimestamp;
    this.txId = ArrayUtils.isEmpty(txId) ? "" : Hex.toHexString(txId);
    this.originAddress =
        ArrayUtils.isEmpty(originAddress) ? "" : Wallet.encode58Check(originAddress);

  }

  public List<ContractTrigger> parseLogInfos(List<LogInfo> logInfos, Deposit deposit) {

    List<ContractTrigger> list = new LinkedList<>();
    if (logInfos == null || logInfos.size() <= 0) {
      return list;
    }

    Map<String, String> signMap = new HashMap<>();
    Map<String, String> abiMap = new HashMap<>();

    for (LogInfo logInfo : logInfos) {

      byte[] contractAddress = MUtil.convertToTronAddress(logInfo.getAddress());
      String strContractAddr =
          ArrayUtils.isEmpty(contractAddress) ? "" : Wallet.encode58Check(contractAddress);
      if (signMap.get(strContractAddr) != null) {
        continue;
      }
      ContractCapsule contract = deposit.getContract(contractAddress);
      if (contract == null) {
        signMap.put(strContractAddr, originAddress); // mark as found.
        abiMap.put(strContractAddr, "");
        continue;
      }
      ABI abi = contract.getInstance().getAbi();
      String creatorAddr = Wallet.encode58Check(
          MUtil.convertToTronAddress(contract.getInstance().getOriginAddress().toByteArray()));
      signMap.put(strContractAddr, creatorAddr); // mark as found.

      if (abi != null && abi.getEntrysCount() > 0) {
        try {
          abiMap
              .put(strContractAddr, JsonFormat.printer().includingDefaultValueFields().print(abi));
        } catch (InvalidProtocolBufferException e) {
          abiMap.put(strContractAddr, "");
          logger.info("abi to json empty:" + txId, e);
        }
      } else {
        abiMap.put(strContractAddr, "");
      }
    }

    int index = 1;
    for (LogInfo logInfo : logInfos) {
      byte[] contractAddress = MUtil.convertToTronAddress(logInfo.getAddress());
      String strContractAddr =
          ArrayUtils.isEmpty(contractAddress) ? "" : Wallet.encode58Check(contractAddress);

      String abiString = abiMap.get(strContractAddr);
      ContractTrigger event = new ContractTrigger();
      String creatorAddr = signMap.get(strContractAddr);
      event.setUniqueId(txId + "_" + index);
      event.setTransactionId(txId);
      event.setContractAddress(strContractAddr);
      event.setOriginAddress(originAddress);
      event.setCallerAddress("");
      event.setCreatorAddress(StringUtils.isEmpty(creatorAddr) ? "" : creatorAddr);
      event.setBlockNumber(blockNum);
      event.setTimeStamp(blockTimestamp);
      event.setLogInfo(logInfo);
      event.setAbiString(abiString);

      list.add(event);
      index++;
    }

    return list;
  }

  public static String getEntrySignature(ABI.Entry entry) {
    String signature = entry.getName() + "(";
    StringBuilder builder = new StringBuilder();
    for (ABI.Entry.Param param : entry.getInputsList()) {
      if (builder.length() > 0) {
        builder.append(",");
      }
      builder.append(param.getType());
    }
    signature += builder.toString() + ")";
    return signature;
  }
}
