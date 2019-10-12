package org.tron.common.utils;

import java.sql.Timestamp;

public class Time {

  public static long getCurrentMillis() {
    return System.currentTimeMillis();
  }

  public static String getTimeString(long time) {
    return new Timestamp(time).toString();
  }
}
