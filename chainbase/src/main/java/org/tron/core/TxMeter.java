package org.tron.core;

import com.google.protobuf.ByteString;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
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

  public static void setTxMeterMetrics(TransactionCapsule trxCap) {
    String type = trxCap.getInstance().getRawData().getContract(0).getType().toString();
    if (CollectionUtils.isNotEmpty(trxCap.getInstance().getSignatureList())) {
      for (ByteString sig : trxCap.getInstance().getSignatureList()) {
        TxMeter.incrSigLength(sig.toByteArray().length);
      }
    }
    Metrics.histogramObserve(MetricKeys.Histogram.DB_BYTES,
            TxMeter.totalReadLength(),
            "read",
            type
    );
    Metrics.histogramObserve(MetricKeys.Histogram.DB_BYTES,
            TxMeter.totalPutLength(),
            "put",
            type
    );
    Metrics.counterInc(MetricKeys.Counter.DB_OP,
            TxMeter.totalReadCount(),
            "read",
            type
    );
    Metrics.counterInc(MetricKeys.Counter.DB_OP,
            TxMeter.totalPutCount(),
            "put",
            trxCap.getInstance().getRawData().getContract(0).getType().toString()
    );
    Metrics.histogramObserve(MetricKeys.Histogram.TX_SIG_BYTES,
            TxMeter.totalSigLength(),
            "sig"
    );
    Metrics.counterInc(MetricKeys.Counter.TX_SIZE_COUNT,
            1,
            type);
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

  private TransactionCapsule transactionCapsule;

  private long putCount;

  private long readCount;

  private long putLength;

  private long readLength;

  private long sigLength;

  private boolean isInit = false;

  public Meter(TransactionCapsule transactionCapsule) {
    this.transactionCapsule = transactionCapsule;
    this.isInit = true;
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
    return putCount == meter.putCount && readCount == meter.readCount && putLength == meter.putLength && readLength == meter.readLength && sigLength == meter.sigLength && isInit == meter.isInit && Objects.equals(transactionCapsule, meter.transactionCapsule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionCapsule, putCount, readCount, putLength, readLength, sigLength, isInit);
  }
}

