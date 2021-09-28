package org.tron.common.logsfilter.capsule;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.logsfilter.Bloom;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.LogFilter;
import org.tron.core.services.jsonrpc.filters.LogFilterAndResult;
import org.tron.core.services.jsonrpc.filters.LogMatch;
import org.tron.protos.Protocol.TransactionInfo;

@Slf4j
@ToString
public class LogsFilterCapsule extends FilterTriggerCapsule {

  @Getter
  @Setter
  private long blockNumber;
  @Getter
  @Setter
  private String blockHash;
  @Getter
  @Setter
  private Bloom bloom;
  @Getter
  @Setter
  private List<TransactionInfo> txInfoList;
  @Getter
  @Setter
  private boolean removed; //if removed == true, then bloom = null

  public LogsFilterCapsule(long blockNumber, String blockHash, Bloom bloom,
      List<TransactionInfo> txInfoList, boolean removed) {
    this.blockNumber = blockNumber;
    this.blockHash = blockHash;
    this.bloom = bloom;
    this.txInfoList = txInfoList;
    this.removed = removed;
  }

  @Override
  public void processFilterTrigger() {
    logger.info("LogsFilterCapsule processFilterTrigger: {}", this.toString());

    Iterator<Entry<String, LogFilterAndResult>>
        it = TronJsonRpcImpl.getEventFilter2Result().entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, LogFilterAndResult> entry = it.next();
      if (entry.getValue().isExpire()) {
        it.remove();
        continue;
      }

      LogFilterAndResult logFilterAndResult = entry.getValue();
      if (bloom != null
          && !logFilterAndResult.getLogFilterWrapper().getLogFilter().matchBloom(bloom)) {
        continue;
      }

      LogFilter logFilter = logFilterAndResult.getLogFilterWrapper().getLogFilter();
      List<LogFilterElement> elements =
          LogMatch.matchBlock(logFilter, blockNumber, null, txInfoList, removed);
      if (CollectionUtils.isNotEmpty(elements)) {
        logFilterAndResult.getResult().addAll(elements);
      }
    }
  }
}