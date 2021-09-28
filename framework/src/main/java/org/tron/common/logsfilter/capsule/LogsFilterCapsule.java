package org.tron.common.logsfilter.capsule;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.logsfilter.Bloom;
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
  private boolean removed;

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
    // todo process logs filter: handle(this)
    logger.info("LogsFilterCapsule processFilterTrigger: {}", this.toString());
  }
}