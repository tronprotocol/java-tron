package org.tron.core.db;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class RecentTransactionItem {
  @Getter
  @Setter
  private long num;

  @Getter
  @Setter
  private List<String> transactionIds;

  public RecentTransactionItem() {}

  public RecentTransactionItem(long num, List<String> transactionIds) {
    this.num = num;
    this.transactionIds = transactionIds;
  }
}
