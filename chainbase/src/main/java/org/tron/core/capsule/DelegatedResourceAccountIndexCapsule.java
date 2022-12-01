package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;

@Slf4j(topic = "capsule")
public class DelegatedResourceAccountIndexCapsule implements
    ProtoCapsule<DelegatedResourceAccountIndex> {

  private DelegatedResourceAccountIndex delegatedResourceAccountIndex;

  public DelegatedResourceAccountIndexCapsule(
      final DelegatedResourceAccountIndex delegatedResourceAccountIndex) {
    this.delegatedResourceAccountIndex = delegatedResourceAccountIndex;
  }

  public DelegatedResourceAccountIndexCapsule(final byte[] data) {
    try {
      this.delegatedResourceAccountIndex = DelegatedResourceAccountIndex.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public DelegatedResourceAccountIndexCapsule(ByteString address) {
    this.delegatedResourceAccountIndex = DelegatedResourceAccountIndex.newBuilder()
        .setAccount(address)
        .build();
  }

  public ByteString getAccount() {
    return this.delegatedResourceAccountIndex.getAccount();
  }

  public void setAccount(ByteString address) {
    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
        .setAccount(address).build();
  }

  public List<ByteString> getFromAccountsList() {
    return this.delegatedResourceAccountIndex.getFromAccountsList();
  }

  public void setAllFromAccounts(List<ByteString> fromAccounts) {
    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
        .clearFromAccounts()
        .addAllFromAccounts(fromAccounts)
        .build();
  }

  public void addFromAccount(ByteString fromAccount) {
    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
        .addFromAccounts(fromAccount)
        .build();
  }

  public void removeFromAccount(ByteString fromAccount) {
    if (getFromAccountsList().contains(fromAccount)) {
      List<ByteString> fromList = new ArrayList<>(getFromAccountsList());
      fromList.remove(fromAccount);
      setAllFromAccounts(fromList);
    }
  }

  public List<ByteString> getToAccountsList() {
    return this.delegatedResourceAccountIndex.getToAccountsList();
  }

  public void setAllToAccounts(List<ByteString> toAccounts) {
    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
        .clearToAccounts()
        .addAllToAccounts(toAccounts)
        .build();
  }

  public void addToAccount(ByteString toAccount) {
    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
        .addToAccounts(toAccount)
        .build();
  }

  public void removeToAccount(ByteString toAccount) {
    if (getToAccountsList().contains(toAccount)) {
      List<ByteString> toList = new ArrayList<>(getToAccountsList());
      toList.remove(toAccount);
      setAllToAccounts(toList);
    }
  }

  public void setTimestamp(long time) {
    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
        .setTimestamp(time)
        .build();
  }

  public long getTimestamp() {
    return this.delegatedResourceAccountIndex.getTimestamp();
  }

  public byte[] createDbKey() {
    return getAccount().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAccount().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.delegatedResourceAccountIndex.toByteArray();
  }

  @Override
  public DelegatedResourceAccountIndex getInstance() {
    return this.delegatedResourceAccountIndex;
  }

}
