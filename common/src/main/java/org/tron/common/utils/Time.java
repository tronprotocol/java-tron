package org.tron.common.utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Time {

  // Matches joda-time's DateTime.toString() output, byte for byte: fixed
  // 3-digit millis, offset as +08:00 (or -00:44:30 when the offset has
  // seconds), and Z when the system zone is UTC.
  private static final DateTimeFormatter ISO_MILLIS_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX");

  public static long getCurrentMillis() {
    return System.currentTimeMillis();
  }

  public static String getTimeString(long time) {
    return new Timestamp(time).toString();
  }

  public static String getIsoTimeString(long time) {
    return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(ISO_MILLIS_FORMAT);
  }
}
