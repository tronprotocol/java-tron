/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.tron.common.cron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;

public class CronExpressionTest {

  private static void assertTrue(boolean condition, String message) {
    if (!condition) {
      fail(message);
    }
  }

  private static void assertTrue(boolean condition) {
    if (!condition) {
      fail();
    }
  }

  @Test
  public void testTooManyTokens() {
    try {
      new CronExpression("0 15 10 * * ? 2005 *"); // too many tokens/terms in expression
      fail("Expected ParseException did not occur for invalid expression");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().contains("too many"),
          "Incorrect ParseException thrown");
    }

  }

  @Test
  public void testIsSatisfiedBy() throws Exception {
    CronExpression cronExpression = new CronExpression("0 15 10 * * ? 2005");

    Calendar cal = Calendar.getInstance();

    cal.set(2005, Calendar.JUNE, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(Calendar.YEAR, 2006);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.JUNE, 1, 10, 16, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.JUNE, 1, 10, 14, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.JUNE, 30, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.MARCH, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(Calendar.DAY_OF_MONTH, 31);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.MARCH, 1, 10, 16, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.MARCH, 1, 10, 14, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.FEBRUARY, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(Calendar.DAY_OF_MONTH, 28);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.FEBRUARY, 1, 10, 16, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal = Calendar.getInstance();
    cal.set(2005, Calendar.FEBRUARY, 1, 10, 14, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 12 * ? 2005");
    cal.set(2005, Calendar.DECEMBER, 12, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.DECEMBER, 11, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.DECEMBER, 13, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
  }

  @Test
  public void testIsSatisfiedByLastDayOfMonth() throws Exception {
    Calendar cal = Calendar.getInstance();

    CronExpression cronExpression = new CronExpression("0 15 10 L * ? 2005");
    cal.set(2005, Calendar.DECEMBER, 31, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.DECEMBER, 30, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.SEPTEMBER, 30, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.SEPTEMBER, 29, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L 2 ? 2005");
    cal.set(2005, Calendar.FEBRUARY, 28, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.FEBRUARY, 27, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L 2 ? 2004");
    cal.set(2004, Calendar.FEBRUARY, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2004, Calendar.FEBRUARY, 28, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
  }

  @Test
  public void testIsSatisfiedByLastDayOfMonthWithOffset() throws Exception {
    Calendar cal = Calendar.getInstance();

    CronExpression cronExpression = new CronExpression("0 15 10 L-2 * ? 2005");
    cal.set(2005, Calendar.DECEMBER, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.DECEMBER, 28, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.DECEMBER, 30, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.DECEMBER, 31, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-1 * ? 2005");
    cal.set(2005, Calendar.SEPTEMBER, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.SEPTEMBER, 27, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.SEPTEMBER, 28, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.SEPTEMBER, 30, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-3 2 ? 2005");
    cal.set(2005, Calendar.FEBRUARY, 25, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.FEBRUARY, 24, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.FEBRUARY, 26, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2005, Calendar.FEBRUARY, 28, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-3 2 ? 2000");
    cal.set(2000, Calendar.FEBRUARY, 26, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2000, Calendar.FEBRUARY, 25, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2000, Calendar.FEBRUARY, 27, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2000, Calendar.FEBRUARY, 29, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
  }

  @Test
  public void testNextValidTimeAfterLastDayOfShortMonth() throws Exception {
    assertNextFireTimesInUtc("0 0 0 L 4 ? 2027", "2027-04-01T00:00:00Z", "2027-04-30T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 L * ? 2027", "2027-04-01T00:00:00Z", "2027-04-30T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 L-1 4 ? 2027", "2027-04-01T00:00:00Z",
        "2027-04-29T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 L 2 ? 2028", "2027-04-01T00:00:00Z", "2028-02-29T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 L 2 ? 2027", "2027-01-01T00:00:00Z", "2027-02-28T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 L-3 2 ? 2028", "2027-04-01T00:00:00Z",
        "2028-02-26T00:00:00Z");
  }

  @Test
  public void testIsValidExpression() {
    assertTrue(CronExpression.isValidExpression("0 0 0 L-2 * ? *"));
    assertTrue(CronExpression.isValidExpression("0 0 0 LW * ? *"));
    assertFalse(CronExpression.isValidExpression("0 0 0 Foo * ? *"));
    assertFalse(CronExpression.isValidExpression("61 15 10 L-2 * ? 2010"));
    assertFalse(CronExpression.isValidExpression("0 61 10 L-2 * ? 2010"));
    assertFalse(CronExpression.isValidExpression("0 15 25 L-2 * ? 2010"));
    assertTrue(CronExpression.isValidExpression("0/5 * * * * ?"));
    assertTrue(CronExpression.isValidExpression("0 0 2 * * ?"));
    assertTrue(CronExpression.isValidExpression("0 15 8 ? * MON-FRI"));
    assertTrue(CronExpression.isValidExpression("0 45 15 1,15 * ? 2005"));
    assertTrue(CronExpression.isValidExpression("0 10 * * * ?"));
    assertTrue(CronExpression.isValidExpression("0 0 12 L 3,6,9,12 ?"));
    assertTrue(CronExpression.isValidExpression("0 0 6 ? DEC,JAN SUN,SAT"));
    assertTrue(CronExpression.isValidExpression("0 0 12 1/5 * ?"));
    assertTrue(CronExpression.isValidExpression("0 0 8-18 ? * MON,WED,FRI"));
    assertTrue(CronExpression.isValidExpression("0 10,44 14 ? 3 WED 2022/2"));
    assertTrue(CronExpression.isValidExpression("0 0/30 9-17 * * ? 2022-2025"));
    assertTrue(CronExpression.isValidExpression("0 15 10 ? * 6#3 2022,2023"));
    assertTrue(CronExpression.isValidExpression("0 10,44 14 ? 3 WED 2022/2"));
    assertTrue(CronExpression.isValidExpression("0 0/5 14,18 * * ?"));
    assertTrue(CronExpression.isValidExpression("0 15 10 ? * 6#3"));
    assertFalse(CronExpression.isValidExpression(" 0 15 10 ? * 6#3 2014-2012"));
    assertTrue(CronExpression.isValidExpression("0 0 20-18 ? * MON,WED,FRI"));
    assertTrue(CronExpression.isValidExpression("0 0/30 17-9 * 10-9 ? 2022"));
    assertTrue(CronExpression.isValidExpression("* * * * * ?"));
    assertTrue(CronExpression.isValidExpression("0 * 5 * * ?"));
    assertTrue(CronExpression.isValidExpression("0 15 10 L-1W,L-1 * ? 2010"));
    assertFalse(CronExpression.isValidExpression("Ralf 30 * * * ?"));
    assertFalse(CronExpression.isValidExpression("0 30 Ralf * * ?"));
    assertFalse(CronExpression.isValidExpression("kilroy was here"));
    assertFalse(CronExpression.isValidExpression("L 30 * * * ?"));
    assertFalse(CronExpression.isValidExpression("0 L * * * ?"));
    assertFalse(CronExpression.isValidExpression("0 0 L * * ?"));
  }

  @Test
  public void testIncrementRangeBoundary() {
    assertEquals("Increment >= 60 : 60", assertThrows(ParseException.class,
        () -> new CronExpression("/60 0 8-18 ? * 2-6")).getMessage());
    assertEquals("Increment >= 60 : 60", assertThrows(ParseException.class,
        () -> new CronExpression("0 /60 8-18 ? * 2-6")).getMessage());
    assertEquals("Increment >= 24 : 24", assertThrows(ParseException.class,
        () -> new CronExpression("0 0 /24 ? * 2-6")).getMessage());
    assertEquals("Increment > 12 : 13", assertThrows(ParseException.class,
        () -> new CronExpression("0 0 0 ? /13 2-6")).getMessage());
  }

  @Test
  public void testLastDayOffset() throws Exception {
    CronExpression cronExpression = new CronExpression("0 15 10 L-2 * ? 2010");
    cronExpression.setTimeZone(Calendar.getInstance().getTimeZone());

    Calendar cal = Calendar.getInstance();
    // last day - 2
    cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 28, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    // last day - 2 for February
    cal.set(2010, Calendar.FEBRUARY, 26, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.FEBRUARY, 25, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    // last day - 2 for June
    cal.set(2010, Calendar.JUNE, 28, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.JUNE, 27, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-5W * ? 2010");
    // last day - 5
    cal.set(2010, Calendar.OCTOBER, 26, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 25, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 27, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    // last day - 5 (September has 30 days)
    cal.set(2010, Calendar.SEPTEMBER, 24, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.SEPTEMBER, 23, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.SEPTEMBER, 25, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-1 * ? 2010");
    // last day - 1
    cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-1W * ? 2010");
    // nearest weekday to last day - 1 (29th is a friday in 2010)
    cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 1,L * ? 2010");

    cal.set(2010, Calendar.OCTOBER, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 31, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.FEBRUARY, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.FEBRUARY, 28, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.FEBRUARY, 27, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 L-1W,L-1 * ? 2010");
    // nearest weekday to last day - 1 (29th is a friday in 2010)
    cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
    // last day - 1
    cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
    // nearest weekday to last day - 1 (26th is a friday in 2010)
    cal.set(2010, Calendar.FEBRUARY, 26, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
    // last day - 1
    cal.set(2010, Calendar.FEBRUARY, 27, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cronExpression = new CronExpression("0 15 10 2W,16 * ? 2010");
    // nearest weekday to the 2nd of the month (1st is a friday in 2010)
    cal.set(2010, Calendar.OCTOBER, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 2, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 16, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
    // 2nd is a Tuesday in November 2010
    cal.set(2010, Calendar.NOVEMBER, 2, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.NOVEMBER, 16, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
  }

  @Test
  public void testQuartz() throws Exception {
    CronExpression cronExpression = new CronExpression("19 15 10 4 Apr ? ");
    assertEquals("19 15 10 4 Apr ? ".toUpperCase(Locale.ROOT), cronExpression.getCronExpression());
    assertEquals("19 15 10 4 Apr ? ".toUpperCase(Locale.ROOT), cronExpression.toString());

    // if broken, this will throw an exception
    cronExpression.getNextValidTimeAfter(new Date());

    try {
      new CronExpression(null);
      fail("Expected ParseException did not fire for null ");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().equals("cronExpression cannot be null"),
          "Incorrect ParseException thrown");
    }

    try {
      new CronExpression("* * * * Foo ? ");
      fail("Expected ParseException did not fire for nonexistent month");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith("Invalid Month value:"),
          "Incorrect ParseException thrown");
    }

    try {
      new CronExpression("* * * * Jan-Foo ? ");
      fail("Expected ParseException did not fire for nonexistent month");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith("Invalid Month value:"),
          "Incorrect ParseException thrown");
    }

    try {
      new CronExpression("0 0 * * * *");
      fail("Expected ParseException did not fire for wildcard day-of-month and day-of-week");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith(
              "Support for specifying both a day-of-week AND"
                  + " a day-of-month parameter is not implemented."),
          "Incorrect ParseException thrown");
    }
    try {
      new CronExpression("0 0 * 4 * *");
      fail("Expected ParseException did not fire for specified day-of-month and"
          + " wildcard day-of-week");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith(
          "Support for specifying both a day-of-week AND a day-of-month"
              + " parameter is not implemented."), "Incorrect ParseException thrown");
    }
    try {
      new CronExpression("0 0 * * * 4");
      fail("Expected ParseException did not fire for wildcard day-of-month"
          + " and specified day-of-week");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith(
          "Support for specifying both a day-of-week AND a day-of-month"
              + " parameter is not implemented."), "Incorrect ParseException thrown");
    }

    try {
      new CronExpression("0 43 9 ? * SAT,SUN,L");
      fail("Expected ParseException did not fire for L combined with other days of the week");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith(
              "Support for specifying 'L' with other days of the week is not implemented"),
          "Incorrect ParseException thrown");
    }
    try {
      new CronExpression("0 43 9 ? * 6,7,L");
      fail("Expected ParseException did not fire for L combined with other days of the week");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith(
              "Support for specifying 'L' with other days of the week is not implemented"),
          "Incorrect ParseException thrown");
    }
    try {
      new CronExpression("0 43 9 ? * 5L");
    } catch (ParseException pe) {
      fail("Unexpected ParseException thrown for supported '5L' expression.");
    }
  }

  @Test
  public void testQtz96() throws ParseException {
    try {
      new CronExpression("0/5 * * 32W 1 ?");
      fail("Expected ParseException did not fire for W with value larger than 31");
    } catch (ParseException pe) {
      assertTrue(pe.getMessage().startsWith(
              "The 'W' option does not make sense with values larger than"),
          "Incorrect ParseException thrown");
    }

    // Test case 1
    try {
      new CronExpression("/120 0 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in '_blank/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment >= 60 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0/120 0 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment >= 60 : 120", e.getMessage());
    }

    // Test case 3
    try {
      new CronExpression("/ 0 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in '_blank/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 4
    try {
      new CronExpression("0/ 0 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in '0/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 1
    try {
      new CronExpression("0 /120 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in '_blank/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment >= 60 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0/120 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment >= 60 : 120", e.getMessage());
    }

    // Test case 3
    try {
      new CronExpression("0 / 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in '_blank/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 4
    try {
      new CronExpression("0 0/ 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in '0/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 1
    try {
      new CronExpression("0 0 /120 ? * 2-6");
      fail("Cron did not validate bad range interval in '_blank/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment >= 24 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0 0/120 ? * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment >= 24 : 120", e.getMessage());
    }

    // Test case 3
    try {
      new CronExpression("0 0 / ? * 2-6");
      fail("Cron did not validate bad range interval in '_blank/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 4
    try {
      new CronExpression("0 0 0/ ? * 2-6");
      fail("Cron did not validate bad range interval in '0/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 1
    try {
      new CronExpression("0 0 0 /120 * 2-6");
      fail("Cron did not validate bad range interval in '_blank/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 31 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0 0 0/120 * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 31 : 120", e.getMessage());
    }

    // Test case 3
    try {
      new CronExpression("0 0 0 / * 2-6");
      fail("Cron did not validate bad range interval in '_blank/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 4
    try {
      new CronExpression("0 0 0 0/ * 2-6");
      fail("Cron did not validate bad range interval in '0/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }
    // Test case 1
    try {
      new CronExpression("0 0 0 ? /120 2-6");
      fail("Cron did not validate bad range interval in '_blank/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 12 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0 0 ? 0/120 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 12 : 120", e.getMessage());
    }

    // Test case 3
    try {
      new CronExpression("0 0 0 ? / 2-6");
      fail("Cron did not validate bad range interval in '_blank/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 4
    try {
      new CronExpression("0 0 0 ? 0/ 2-6");
      fail("Cron did not validate bad range interval in '0/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }
    // Test case 1
    try {
      new CronExpression("0 0 0 ? * /120");
      fail("Cron did not validate bad range interval in '_blank/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 7 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0 0 ? * 0/120");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 7 : 120", e.getMessage());
    }

    // Test case 3
    try {
      new CronExpression("0 0 0 ? * /");
      fail("Cron did not validate bad range interval in '_blank/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }

    // Test case 4
    try {
      new CronExpression("0 0 0 ? * 0/");
      fail("Cron did not validate bad range interval in '0/_blank'");
    } catch (ParseException e) {
      assertEquals("'/' must be followed by an integer.", e.getMessage());
    }
  }

  @Test
  public void test1420() throws Exception {
    Date after = localTime(2025, Calendar.NOVEMBER, 18, 10, 15, 0);

    assertNextFireTimes(new CronExpression("0 0 0 1W * ?"), after,
        localTime(2025, Calendar.DECEMBER, 1, 0, 0, 0),
        localTime(2026, Calendar.JANUARY, 1, 0, 0, 0),
        localTime(2026, Calendar.FEBRUARY, 2, 0, 0, 0),
        localTime(2026, Calendar.MARCH, 2, 0, 0, 0),
        localTime(2026, Calendar.APRIL, 1, 0, 0, 0),
        localTime(2026, Calendar.MAY, 1, 0, 0, 0),
        localTime(2026, Calendar.JUNE, 1, 0, 0, 0),
        localTime(2026, Calendar.JULY, 1, 0, 0, 0),
        localTime(2026, Calendar.AUGUST, 3, 0, 0, 0),
        localTime(2026, Calendar.SEPTEMBER, 1, 0, 0, 0),
        localTime(2026, Calendar.OCTOBER, 1, 0, 0, 0),
        localTime(2026, Calendar.NOVEMBER, 2, 0, 0, 0),
        localTime(2026, Calendar.DECEMBER, 1, 0, 0, 0));

    assertNextFireTimes(new CronExpression("59 59 23 L * ?"), after,
        localTime(2025, Calendar.NOVEMBER, 30, 23, 59, 59),
        localTime(2025, Calendar.DECEMBER, 31, 23, 59, 59),
        localTime(2026, Calendar.JANUARY, 31, 23, 59, 59),
        localTime(2026, Calendar.FEBRUARY, 28, 23, 59, 59),
        localTime(2026, Calendar.MARCH, 31, 23, 59, 59),
        localTime(2026, Calendar.APRIL, 30, 23, 59, 59),
        localTime(2026, Calendar.MAY, 31, 23, 59, 59),
        localTime(2026, Calendar.JUNE, 30, 23, 59, 59),
        localTime(2026, Calendar.JULY, 31, 23, 59, 59),
        localTime(2026, Calendar.AUGUST, 31, 23, 59, 59),
        localTime(2026, Calendar.SEPTEMBER, 30, 23, 59, 59),
        localTime(2026, Calendar.OCTOBER, 31, 23, 59, 59),
        localTime(2026, Calendar.NOVEMBER, 30, 23, 59, 59),
        localTime(2026, Calendar.DECEMBER, 31, 23, 59, 59));

    assertTrue(CronExpression.isValidExpression("0 0 0 1W * ?"));
    assertTrue(CronExpression.isValidExpression("59 59 23 L * ?"));
  }

  @Test
  public void testNearestWeekdayOnLaterDayStartsAtFirstHour() throws Exception {
    assertNextFireTimesInUtc("0 0 0,12 15W * ?", "2027-06-01T00:01:00Z",
        "2027-06-15T00:00:00Z", "2027-06-15T12:00:00Z", "2027-07-15T00:00:00Z");
  }

  @Test
  public void testNearestWeekdayOnLaterDayStartsAtFirstMinute() throws Exception {
    assertNextFireTimesInUtc("0 * * 15W * ?", "2027-06-01T00:01:00Z",
        "2027-06-15T00:00:00Z", "2027-06-15T00:01:00Z");
  }

  @Test
  public void testNearestWeekdayOnLaterDayStartsAtFirstSecond() throws Exception {
    assertNextFireTimesInUtc("*/20 * * 15W * ?", "2027-06-01T00:01:00Z",
        "2027-06-15T00:00:00Z", "2027-06-15T00:00:20Z", "2027-06-15T00:00:40Z");
  }

  @Test
  public void testNearestWeekdayMovedBackStartsAtFirstFireTime() throws Exception {
    assertNextFireTimesInUtc("0 0 0,12 15W * ?", "2026-08-01T00:01:00Z",
        "2026-08-14T00:00:00Z", "2026-08-14T12:00:00Z", "2026-09-15T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0,12 15W * ?", "2026-08-15T00:00:00Z",
        "2026-09-15T00:00:00Z");
    assertNextFireTimesInUtc("0 0 12 LW 10 ? 2027", "2027-10-01T00:00:00Z",
        "2027-10-29T12:00:00Z");
  }

  @Test
  public void testLastWeekdayOnLaterDayStartsAtFirstFireTime() throws Exception {
    assertNextFireTimesInUtc("0 0 0,12 LW * ?", "2027-07-01T00:01:00Z",
        "2027-07-30T00:00:00Z", "2027-07-30T12:00:00Z", "2027-08-31T00:00:00Z");
  }

  @Test
  public void testLastDayOffsetWeekdayOnLaterDayStartsAtFirstFireTime() throws Exception {
    assertNextFireTimesInUtc("0 0/30 9 L-2W * ?", "2027-07-01T09:10:00Z",
        "2027-07-29T09:00:00Z", "2027-07-29T09:30:00Z");
    assertNextFireTimesInUtc("0 0 0,12 L-1W 10 ? 2027", "2027-10-01T00:00:00Z",
        "2027-10-29T00:00:00Z", "2027-10-29T12:00:00Z");
    assertNextFireTimesInUtc("0 0 0,12 L-2W 8 ? 2027", "2027-08-01T00:00:00Z",
        "2027-08-30T00:00:00Z", "2027-08-30T12:00:00Z");
  }

  @Test
  public void testNearestWeekdayMovedForwardIsSatisfied() throws Exception {
    assertNextFireTimesInUtc("0 0 0 1W 5 ? 2027", "2027-04-30T12:00:00Z",
        "2027-05-03T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 1W 5 ? 2027", "2027-05-02T00:00:00Z",
        "2027-05-03T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0,12 15W * ?", "2027-08-01T00:00:00Z",
        "2027-08-16T00:00:00Z", "2027-08-16T12:00:00Z", "2027-09-15T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0 1W * ?", "2027-05-01T00:00:00Z", "2027-05-03T00:00:00Z");
    assertNextFireTimesInUtc("0 0 0,12 1W 5 ? 2027", "2027-05-03T00:00:00Z",
        "2027-05-03T12:00:00Z");
  }

  @Test
  public void testNearestWeekdayListDoesNotSkipMonth() throws Exception {
    assertNextFireTimesInUtc("0 0 10 3W,20W 4 ?", "2027-04-02T10:00:00Z",
        "2027-04-20T10:00:00Z", "2028-04-03T10:00:00Z");
  }

  @Test(timeout = 10000)
  public void testNearestWeekdayInDaylightSavingGapTerminates() throws Exception {
    CronExpression cronExpression = new CronExpression("0 30 1 29W 2 ?");
    cronExpression.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));
    Date after = Date.from(Instant.parse("2003-02-28T12:00:00Z"));

    Date next = cronExpression.getNextValidTimeAfter(after);
    assertEquals(Instant.parse("2004-02-26T23:30:00Z"), next.toInstant());
    assertTrue(cronExpression.isSatisfiedBy(next));
  }

  @Test
  public void testNearestWeekdayBeforeLaterDayOfMonthStillFires() throws Exception {
    assertNextFireTimesInUtc("18 0 22 L,3W,LW 3 ? 2001", "2001-03-02T22:00:18Z",
        "2001-03-30T22:00:18Z", "2001-03-31T22:00:18Z");
    assertNextFireTimesInUtc("0 0 10 5,20W 4 ? 2027", "2027-04-01T00:00:00Z",
        "2027-04-05T10:00:00Z", "2027-04-20T10:00:00Z");
  }

  @Test(timeout = 10000)
  public void testNearestWeekdayDoesNotFireInDaylightSavingGap() throws Exception {
    TimeZone amman = TimeZone.getTimeZone("Asia/Amman");
    Date after = Date.from(Instant.parse("2021-03-20T00:00:00Z"));

    CronExpression cronExpression = new CronExpression("0 30 0 27W 3 ? 2021");
    cronExpression.setTimeZone(amman);
    assertNull(cronExpression.getNextValidTimeAfter(after));
    assertFalse(cronExpression.isSatisfiedBy(Date.from(Instant.parse("2021-03-25T22:30:00Z"))));

    cronExpression = new CronExpression("0 30 0,5 27W 3 ? 2021");
    cronExpression.setTimeZone(amman);
    Date next = cronExpression.getNextValidTimeAfter(after);
    assertEquals(Instant.parse("2021-03-26T02:30:00Z"), next.toInstant());
    assertTrue(cronExpression.isSatisfiedBy(next));
  }

  @Test(timeout = 10000)
  public void testNearestWeekdayOnSkippedDayNeverGoesBackwards() throws Exception {
    CronExpression cronExpression = new CronExpression("0 5 0,2 LW,L,1 12 ? 2011");
    cronExpression.setTimeZone(TimeZone.getTimeZone("Pacific/Apia"));
    Date after = Date.from(Instant.parse("2011-12-30T10:05:00Z"));

    Date next = cronExpression.getNextValidTimeAfter(after);
    assertEquals(Instant.parse("2011-12-30T12:05:00Z"), next.toInstant());
    assertTrue(cronExpression.isSatisfiedBy(next));
  }

  @Test
  public void testNearestWeekdayIgnoresDefaultCalendarSystem() throws Exception {
    Locale defaultLocale = Locale.getDefault();
    try {
      for (Locale locale : new Locale[] {new Locale("th", "TH"), new Locale("ja", "JP", "JP")}) {
        Locale.setDefault(locale);
        assertNextFireTimesInUtc("0 0 0 15W * ?", "2027-06-01T00:00:00Z",
            "2027-06-15T00:00:00Z", "2027-07-15T00:00:00Z");
      }
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void testNextFireTimeIgnoresMilliseconds() throws Exception {
    CronExpression cronExpression = new CronExpression("* * * * * ?");
    cronExpression.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date next = cronExpression.getNextValidTimeAfter(
        Date.from(Instant.parse("2027-06-01T00:00:00.700Z")));
    assertEquals(Instant.parse("2027-06-01T00:00:01Z"), next.toInstant());
    assertTrue(cronExpression.isSatisfiedBy(next));

    cronExpression = new CronExpression("0 0 12 * * ?");
    cronExpression.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertTrue(cronExpression.isSatisfiedBy(Date.from(Instant.parse("2027-06-01T12:00:00.500Z"))));
    assertFalse(cronExpression.isSatisfiedBy(Date.from(Instant.parse("2027-06-01T12:00:01Z"))));
  }

  private static Date localTime(int year, int month, int day, int hour, int minute,
      int second) {
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(year, month, day, hour, minute, second);
    return cal.getTime();
  }

  private static void assertNextFireTimes(CronExpression cronExpression, Date after,
      Date... expected) {
    Date time = after;
    for (Date next : expected) {
      time = cronExpression.getTimeAfter(time);
      assertEquals(next, time);
      assertTrue(cronExpression.isSatisfiedBy(time));
    }
  }

  private static void assertNextFireTimesInUtc(String expression, String after,
      String... expected) throws ParseException {
    CronExpression cronExpression = new CronExpression(expression);
    cronExpression.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date time = Date.from(Instant.parse(after));
    for (String next : expected) {
      time = cronExpression.getNextValidTimeAfter(time);
      assertEquals(Instant.parse(next), time.toInstant());
      assertTrue(cronExpression.isSatisfiedBy(time), next);
    }
  }
}
