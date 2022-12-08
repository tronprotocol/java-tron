package org.tron.core.capsule;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.DelegatedResource;

@Slf4j(topic = "capsule")
public class DelegatedResourceCapsule implements ProtoCapsule<DelegatedResource> {

  protected static final byte[] V2_PREFIX = new byte[]{0x01};
  protected static final byte[] V2_LOCK_PREFIX = new byte[]{0x02};

  private DelegatedResource delegatedResource;

  public DelegatedResourceCapsule(final DelegatedResource delegatedResource) {
    this.delegatedResource = delegatedResource;
  }

  public DelegatedResourceCapsule(final byte[] data) {
    try {
      this.delegatedResource = DelegatedResource.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public DelegatedResourceCapsule(ByteString from, ByteString to) {
    this.delegatedResource = DelegatedResource.newBuilder()
        .setFrom(from)
        .setTo(to)
        .build();
  }

  public static byte[] createDbKey(byte[] from, byte[] to) {
    byte[] key = new byte[from.length + to.length];
    System.arraycopy(from, 0, key, 0, from.length);
    System.arraycopy(to, 0, key, from.length, to.length);
    return key;
  }

  public static byte[] createDbKeyV2(byte[] from, byte[] to, boolean lock) {
    if (lock) {
      return Bytes.concat(V2_LOCK_PREFIX, from, to);
    }
    return Bytes.concat(V2_PREFIX, from, to);
  }

  public ByteString getFrom() {
    return this.delegatedResource.getFrom();
  }

  public ByteString getTo() {
    return this.delegatedResource.getTo();
  }

  public long getFrozenBalanceForEnergy() {
    return this.delegatedResource.getFrozenBalanceForEnergy();
  }

  public void setFrozenBalanceForEnergy(long energy, long expireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForEnergy(energy)
        .setExpireTimeForEnergy(expireTime)
        .build();
  }

  public void addFrozenBalanceForEnergy(long energy, long expireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForEnergy(this.delegatedResource.getFrozenBalanceForEnergy() + energy)
        .setExpireTimeForEnergy(expireTime)
        .build();
  }

  public long getFrozenBalanceForBandwidth() {
    return this.delegatedResource.getFrozenBalanceForBandwidth();
  }

  public long getFrozenBalance(boolean isBandwidth) {
    if (isBandwidth) {
      return getFrozenBalanceForBandwidth();
    } else {
      return getFrozenBalanceForEnergy();
    }

  }

  public void setFrozenBalanceForBandwidth(long bandwidth, long expireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForBandwidth(bandwidth)
        .setExpireTimeForBandwidth(expireTime)
        .build();
  }

  public void addFrozenBalanceForBandwidth(long bandwidth, long expireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForBandwidth(this.delegatedResource.getFrozenBalanceForBandwidth()
            + bandwidth)
        .setExpireTimeForBandwidth(expireTime)
        .build();
  }

  public long getExpireTimeForBandwidth() {
    return this.delegatedResource.getExpireTimeForBandwidth();
  }

  public long getExpireTimeForEnergy() {
    return this.delegatedResource.getExpireTimeForEnergy();
  }

  public void setExpireTimeForBandwidth(long expireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setExpireTimeForBandwidth(expireTime)
        .build();
  }

  public long getExpireTimeForEnergy(DynamicPropertiesStore dynamicPropertiesStore) {
    if (dynamicPropertiesStore.getAllowMultiSign() == 0) {
      return this.delegatedResource.getExpireTimeForBandwidth();
    } else {
      return this.delegatedResource.getExpireTimeForEnergy();
    }
  }

  public void setExpireTimeForEnergy(long expireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setExpireTimeForEnergy(expireTime)
        .build();
  }

  public byte[] createDbKey() {
    return createDbKey(this.delegatedResource.getFrom().toByteArray(),
        this.delegatedResource.getTo().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.delegatedResource.toByteArray();
  }

  @Override
  public DelegatedResource getInstance() {
    return this.delegatedResource;
  }

}
