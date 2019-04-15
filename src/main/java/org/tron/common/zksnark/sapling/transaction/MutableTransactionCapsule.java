package org.tron.common.zksnark.sapling.transaction;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class MutableTransactionCapsule {
  @Getter
  @Setter
  private long valueBalance;
  @Getter
  private List<SpendDescriptionCapsule> spends = new ArrayList<>();
  @Getter
  private List<ReceiveDescriptionCapsule> receives = new ArrayList<>();

  public long getAndAddBalance(long value) {
    long v = valueBalance;
    valueBalance += value;
    return v;
  }
}
