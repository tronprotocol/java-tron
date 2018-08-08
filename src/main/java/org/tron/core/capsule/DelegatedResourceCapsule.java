package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.DelegatedResource;

@Slf4j
public class DelegatedResourceCapsule implements ProtoCapsule<DelegatedResource> {

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

  public DelegatedResourceCapsule(ByteString from, ByteString to, long cpu, long bandwidth,
      long expireTime) {
    this.delegatedResource = DelegatedResource.newBuilder()
        .setFrom(from)
        .setTo(to)
        .setFrozenBalanceForCpu(cpu)
        .setFrozenBalanceForBandwidth(bandwidth)
        .setExpireTime(expireTime)
        .build();
  }


  public ByteString getFrom() {
    return this.delegatedResource.getFrom();
  }

  public ByteString getTo() {
    return this.delegatedResource.getTo();
  }

  public long getFrozenBalanceForCpu() {
    return this.delegatedResource.getFrozenBalanceForCpu();
  }

  public void setFrozenBalanceForCpu(long cpu) {
    this.delegatedResource = this.delegatedResource.toBuilder().setFrozenBalanceForCpu(cpu).build();
  }

  public void addFrozenBalanceForCpu(long cpu) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForCpu(this.delegatedResource.getFrozenBalanceForCpu() + cpu).build();
  }

  public long getFrozenBalanceForBandwidth() {
    return this.delegatedResource.getFrozenBalanceForBandwidth();
  }

  public void setFrozenBalanceForBandwidth(long Bandwidth) {
    this.delegatedResource = this.delegatedResource.toBuilder().setFrozenBalanceForBandwidth(Bandwidth).build();
  }

  public void addFrozenBalanceForBandwidth(long Bandwidth) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForBandwidth(this.delegatedResource.getFrozenBalanceForBandwidth() + Bandwidth).build();
  }

  public void addResource(long Bandwidth, long cpu, long ExpireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder()
        .setFrozenBalanceForBandwidth(this.delegatedResource.getFrozenBalanceForBandwidth() + Bandwidth)
        .setFrozenBalanceForCpu(this.delegatedResource.getFrozenBalanceForCpu() + cpu)
        .setExpireTime(ExpireTime).build();
  }

  public long getExpireTime() {
    return this.delegatedResource.getExpireTime();
  }

  public void setExpireTime(long ExpireTime) {
    this.delegatedResource = this.delegatedResource.toBuilder().setExpireTime(ExpireTime).build();
  }

  public byte[] createDbKey() {
    return createDbKey(this.delegatedResource.getFrom().toByteArray(),
        this.delegatedResource.getTo().toByteArray());
  }

  public static byte[] createDbKey(byte[] from, byte[] to) {
    byte[] key = new byte[from.length + to.length];
    System.arraycopy(from, 0, key, 0, from.length);
    System.arraycopy(to, 0, key, from.length, to.length);
    return key;
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
