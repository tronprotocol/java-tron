package org.tron.core.meter;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.capsule.TransactionCapsule;

import java.util.Objects;

public class TxMeter {

  private static final ThreadLocal<Meter> cache = new ThreadLocal<>();

  private TxMeter(){}

  public static void init (TransactionCapsule capsule) {
    Meter meter = new Meter(capsule);
    cache.set(meter);
  }

  public static boolean checkInit() {
    Meter meter = cache.get();
    return meter != null && meter.isInit();
  }

  public static void incrWriteLength(long length) {
    if (!checkInit()) {
      return;
    }
    Meter meter = cache.get();
    meter.incrPutLength(length);
    incrPutCount();
  }

  public static void incrReadLength(long length) {
    if (!checkInit()) {
      return;
    }
    Meter meter = cache.get();
    meter.incrReadLength(length);
    incrReadCount();
  }

  public static void incrReadLength(ProtoCapsule protoCapsule) {
    if (!checkInit() || protoCapsule == null) {
      return;
    }
    incrReadLength(protoCapsule.getData().length);
  }

  public static void incrSigLength(long length) {
    if (!checkInit()) {
      return;
    }
    Meter meter = cache.get();
    meter.incrSigLength(length);
  }

  public static void incrReadCount() {
    Meter meter = cache.get();
    meter.incrReadCount();
  }

  public static void incrPutCount() {
    Meter meter = cache.get();
    meter.incrPutCount();
  }

  public static long totalReadLength() {
    if (!checkInit()) {
      return 0;
    }
    return cache.get().getReadLength();
  }

  public static long totalPutLength() {
    if (!checkInit()) {
      return 0;
    }
    return cache.get().getPutLength();
  }

  public static long totalReadCount() {
    if (!checkInit()) {
      return 0;
    }
    return cache.get().getReadCount();
  }

  public static long totalSigLength() {
    if (!checkInit()) {
      return 0;
    }
    return cache.get().getSigLength();
  }

  public static long totalPutCount() {
    if (!checkInit()) {
      return 0;
    }
    return cache.get().getPutCount();
  }

  public static void remove() {
    if (!checkInit()) {
      return;
    }
    cache.remove();
  }

  public enum BaseType {
    LONG(8),
    BOOLEAN(1),
    ;

    private int length;

    BaseType(int length) {
      this.length = length;
    }

    public int getLength() {
      return length;
    }
  }

}

class Meter {

  private Sha256Hash transactionId;

  private TransactionCapsule transactionCapsule;

  private long putCount;

  private long readCount;

  private long putLength;

  private long readLength;

  private long sigLength;

  private boolean isInit = false;

  public Meter(Sha256Hash transactionId) {
    this.transactionId = transactionId;
    this.isInit = true;
  }

  public Meter(TransactionCapsule transactionCapsule) {
    this.transactionCapsule = transactionCapsule;
    this.isInit = true;
  }

  public Sha256Hash getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Sha256Hash transactionId) {
    this.transactionId = transactionId;
  }

  public long getPutCount() {
    return putCount;
  }

  public void incrPutCount() {
    this.putCount = this.putCount + 1;
  }

  public long getReadCount() {
    return readCount;
  }

  public void incrReadCount() {
    this.readCount = this.readCount + 1;
  }

  public long getPutLength() {
    return putLength;
  }

  public void incrPutLength(long putLength) {
    this.putLength += putLength;
  }

  public long getReadLength() {
    return readLength;
  }

  public void incrReadLength(long readLength) {
    this.readLength += readLength;
  }

  public boolean isInit() {
    return isInit;
  }

  public void setInit(boolean init) {
    isInit = init;
  }

  public long getSigLength() {
    return sigLength;
  }

  public void incrSigLength(long sigLength) {
    this.sigLength += sigLength;
  }

  public TransactionCapsule getTransactionCapsule() {
    return transactionCapsule;
  }

  public void setTransactionCapsule(TransactionCapsule transactionCapsule) {
    this.transactionCapsule = transactionCapsule;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Meter meter = (Meter) o;
    return putCount == meter.putCount && readCount == meter.readCount && putLength == meter.putLength && Objects.equals(transactionId, meter.transactionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionId, putCount, readCount, putLength);
  }
}

