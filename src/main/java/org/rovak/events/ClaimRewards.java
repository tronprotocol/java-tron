package org.rovak.events;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol;

import java.util.List;

public class ClaimRewards extends Event {

  private String eventType = "claim_rewards";

  @Getter
  private String address;

  @Getter
  @Setter
  private Long rewards;

  @Getter
  @Setter
  private Long balanceBefore;

  @Getter
  @Setter
  private Long balanceAfter;

  public ClaimRewards(String address) {
    this.address = address;
  }
}
