package org.rovak.events;

import lombok.Getter;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol;

import java.util.List;

public class AccountVoted extends Event {

  private String eventType = "account_voted";

  @Getter
  private String address;

  @Getter
  private List<Protocol.Vote> votes;

  public AccountVoted(AccountCapsule account) {
    this.address = ByteArray.toHexString(account.getAddress().toByteArray());
    this.votes = account.getVotesList();
  }
}
