package org.tron.common.utils;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TimeTest {

  private static final long YEAR_1900 = -2208988800000L;
  private static final long EPOCH = 0L;
  private static final long BLOCK_TIME = 1700000000123L;

  private TimeZone defaultZone;

  @Before
  public void saveDefaultZone() {
    defaultZone = TimeZone.getDefault();
  }

  @After
  public void restoreDefaultZone() {
    TimeZone.setDefault(defaultZone);
  }

  @Test
  public void testOffsetSecondsArePreserved() {
    useZone("Africa/Monrovia");
    assertEquals("1969-12-31T23:15:30.000-00:44:30", Time.getIsoTimeString(EPOCH));
  }

  @Test
  public void testMinuteOffsetsKeepJodaFormat() {
    useZone("UTC");
    assertEquals("1970-01-01T00:00:00.000Z", Time.getIsoTimeString(EPOCH));
    useZone("Asia/Shanghai");
    assertEquals("2023-11-15T06:13:20.123+08:00", Time.getIsoTimeString(BLOCK_TIME));
  }

  @Test
  public void testEveryZoneRoundTripsToSameInstant() {
    for (String zone : ZoneId.getAvailableZoneIds()) {
      useZone(zone);
      for (long time : new long[] {YEAR_1900, EPOCH, BLOCK_TIME}) {
        String text = Time.getIsoTimeString(time);
        assertEquals(zone + " " + text, time,
            OffsetDateTime.parse(text).toInstant().toEpochMilli());
      }
    }
  }

  private static void useZone(String zone) {
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)));
  }
}
