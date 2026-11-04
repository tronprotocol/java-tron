package org.tron.common.cron;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Test;

public class CronExpressionLocaleTest {

  public static void main(String[] args) throws Exception {
    CronExpression cronExpression = new CronExpression("0 0 12 1 1 ?");
    cronExpression.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date next = cronExpression.getNextValidTimeAfter(
        Date.from(Instant.parse("2026-06-01T00:00:00Z")));
    System.out.print((CronExpression.MAX_YEAR - new GregorianCalendar().get(Calendar.YEAR))
        + " " + next.toInstant() + " " + cronExpression.isSatisfiedBy(next));
  }

  @Test(timeout = 60000)
  public void testYearLimitIgnoresDefaultCalendarSystem() throws Exception {
    String expected = "100 2027-01-01T12:00:00Z true";
    assertEquals(expected, runWithDefaultLocale("ja", "JP", "JP"));
    assertEquals(expected, runWithDefaultLocale("th", "TH", ""));
  }

  private static String runWithDefaultLocale(String language, String country, String variant)
      throws Exception {
    String classPath = location(CronExpression.class) + File.pathSeparator
        + location(CronExpressionLocaleTest.class);
    List<String> command = Arrays.asList(
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
        "-Duser.language=" + language, "-Duser.country=" + country,
        "-Duser.variant=" + variant, "-cp", classPath, CronExpressionLocaleTest.class.getName());
    ProcessBuilder builder = new ProcessBuilder(command);
    Map<String, String> environment = builder.environment();
    environment.remove("JAVA_TOOL_OPTIONS");
    environment.remove("_JAVA_OPTIONS");
    environment.remove("JDK_JAVA_OPTIONS");
    Process process = builder.start();
    String output = read(process.getInputStream());
    String error = read(process.getErrorStream());
    assertEquals(error, 0, process.waitFor());
    return output;
  }

  private static String location(Class<?> type) throws Exception {
    return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
  }

  private static String read(InputStream in) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    for (int n = in.read(buffer); n != -1; n = in.read(buffer)) {
      out.write(buffer, 0, n);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }
}
