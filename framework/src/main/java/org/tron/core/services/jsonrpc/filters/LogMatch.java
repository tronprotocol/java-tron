package org.tron.core.services.jsonrpc.filters;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.JsonRpcTooManyResultException;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionRet;

/**
 * match events from possible blocks one by one
 */
@Slf4j(topic = "API")
public class LogMatch {

  /**
   * query criteria
   */
  private final LogFilterWrapper logFilterWrapper;
  /**
   * possible block number list
   */
  private final List<Long> blockNumList;
  private final Manager manager;

  public LogMatch(LogFilterWrapper logFilterWrapper, List<Long> blockNumList, Manager manager) {
    this.logFilterWrapper = logFilterWrapper;
    this.blockNumList = blockNumList;
    this.manager = manager;
  }

  public static List<LogFilterElement> matchBlock(LogFilter logFilter, long blockNum,
      String blockHash, List<TransactionInfo> transactionInfoList, boolean removed) {

    int txCount = transactionInfoList.size();
    List<LogFilterElement> matchedLog = new ArrayList<>();
    int logIndexInBlock = 0;

    for (int i = 0; i < txCount; i++) {
      TransactionInfo transactionInfo = transactionInfoList.get(i);
      int logCount = transactionInfo.getLogCount();

      for (int j = 0; j < logCount; j++) {
        Log log = transactionInfo.getLog(j);

        if (logFilter.matchesExactly(log)) {
          List<DataWord> topicList = new ArrayList<>();
          for (ByteString topic : log.getTopicsList()) {
            topicList.add(new DataWord(topic.toByteArray()));
          }

          LogFilterElement logFilterElement = new LogFilterElement(blockHash,
              blockNum,
              ByteArray.toHexString(transactionInfo.getId().toByteArray()),
              i,
              ByteArray.toHexString(log.getAddress().toByteArray()),
              topicList,
              ByteArray.toHexString(log.getData().toByteArray()),
              logIndexInBlock,
              removed
          );
          matchedLog.add(logFilterElement);
        }

        logIndexInBlock += 1;
      }
    }

    return matchedLog;
  }

  public LogFilterElement[] matchBlockOneByOne()
      throws BadItemException, ItemNotFoundException, JsonRpcTooManyResultException {
    List<LogFilterElement> logFilterElementList = new ArrayList<>();

    for (long blockNum : blockNumList) {
      TransactionRetCapsule transactionRetCapsule =
          manager.getTransactionRetStore()
              .getTransactionInfoByBlockNum(ByteArray.fromLong(blockNum));
      if (transactionRetCapsule == null) {
        //if query condition (address and topics) is empty, we will traversal every block,
        //include empty block
        continue;
      }
      TransactionRet transactionRet = transactionRetCapsule.getInstance();
      List<TransactionInfo> transactionInfoList = transactionRet.getTransactioninfoList();

      String blockHash = manager.getChainBaseManager().getBlockIdByNum(blockNum).toString();
      List<LogFilterElement> matchedLog = matchBlock(logFilterWrapper.getLogFilter(), blockNum,
          blockHash, transactionInfoList, false);
      if (!matchedLog.isEmpty()) {
        logFilterElementList.addAll(matchedLog);
      }

      if (logFilterElementList.size() > LogBlockQuery.MAX_RESULT) {
        throw new JsonRpcTooManyResultException(
            "query returned more than " + LogBlockQuery.MAX_RESULT + " results");
      }
    }

    return logFilterElementList.toArray(new LogFilterElement[0]);
  }

}
