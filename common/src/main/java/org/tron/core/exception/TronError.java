package org.tron.core.exception;

import lombok.Getter;

/**
 * If a {@link TronError} is thrown, the service will trigger {@link System#exit(int)} by
 * {@link Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}.
 * NOTE: Do not attempt to catch {@link TronError}.
 */
@Getter
public class TronError extends Error {

  private final ErrCode errCode;

  public TronError(String message, ErrCode exitCode) {
    super(message);
    this.errCode = exitCode;
  }

  public TronError(String message, Throwable cause, ErrCode exitCode) {
    super(message, cause);
    this.errCode = exitCode;
  }

  public TronError(Throwable cause, ErrCode exitCode) {
    super(cause);
    this.errCode = exitCode;
  }

  @Getter
  public enum ErrCode {
    WITNESS_KEYSTORE_LOAD(-1),
    CHECKPOINT_VERSION(-1),
    LEVELDB_INIT(1),
    ROCKSDB_INIT(1),
    DB_FLUSH(1),
    REWARD_VI_CALCULATOR(1),
    KHAOS_DB_INIT(1),
    GENESIS_BLOCK_INIT(1),
    EVENT_SUBSCRIBE_ERROR(1),
    AUTO_STOP_PARAMS(1),
    API_SERVER_INIT(1),
    EVENT_SUBSCRIBE_INIT(1),
    PROMETHEUS_INIT(1),
    TRON_NET_SERVICE_INIT(1),
    ZCASH_INIT(1),
    LOG_LOAD(1),
    WITNESS_INIT(1),
    RATE_LIMITER_INIT(1),
    SOLID_NODE_INIT(0);

    private final int code;

    ErrCode(int code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return name() + "(" + code + ")";
    }
  }
}
