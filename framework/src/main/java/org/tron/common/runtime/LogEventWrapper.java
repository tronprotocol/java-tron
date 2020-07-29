package org.tron.common.runtime;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.core.vm.utils.MUtil;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.Param;

public class LogEventWrapper extends ContractTrigger {

  @Getter
  @Setter
  private List<byte[]> topicList;

  @Getter
  @Setter
  private byte[] data;

  /**
   * decode from sha3($EventSignature) with the ABI of this contract.
   */
  @Getter
  @Setter
  private String eventSignature;

  /**
   * ABI Entry of this event.
   */
  @Getter
  @Setter
  private SmartContract.ABI.Entry abiEntry;

  public LogEventWrapper() {
    super();
  }

  public String getEventSignatureFull() {
    if (Objects.isNull(abiEntry)) {
      return "fallback()";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(abiEntry.getName()).append("(");
    StringBuilder sbp = new StringBuilder();
    for (Param param : abiEntry.getInputsList()) {
      if (sbp.length() > 0) {
        sbp.append(",");
      }
      sbp.append(param.getType());
      if (MUtil.isNotNullOrEmpty(param.getName())) {
        sbp.append(" ").append(param.getName());
      }
    }
    sb.append(sbp.toString()).append(")");
    return sb.toString();
  }
}
