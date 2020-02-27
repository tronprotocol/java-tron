package org.tron.core.metrics.blockchain;

public class StartTimeRecorder {
  private long startRecordTime;

  private StartTimeRecorder() {

  }

  private static final StartTimeRecorder startTimeRecorder = new StartTimeRecorder();

  public long getStartRecordTime() {
    return startRecordTime;
  }

  public void setStartRecordTime(long startRecordTime) {
    this.startRecordTime = startRecordTime;
  }

  public static final StartTimeRecorder getInstance() {
    return startTimeRecorder;
  }
}
