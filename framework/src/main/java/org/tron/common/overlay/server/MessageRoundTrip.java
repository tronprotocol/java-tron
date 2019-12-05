package org.tron.common.overlay.server;

import org.tron.common.overlay.message.Message;

public class MessageRoundTrip {

  private final Message msg;
  private long time = 0;
  private long retryTimes = 0;

  public MessageRoundTrip(Message msg) {
    this.msg = msg;
    saveTime();
  }

  public long getRetryTimes() {
    return retryTimes;
  }

  public void incRetryTimes() {
    ++retryTimes;
  }

  public void saveTime() {
    time = System.currentTimeMillis();
  }

  public long getTime() {
    return time;
  }

  public boolean hasToRetry() {
    return 20000 < System.currentTimeMillis() - time;
  }

  public Message getMsg() {
    return msg;
  }
}
