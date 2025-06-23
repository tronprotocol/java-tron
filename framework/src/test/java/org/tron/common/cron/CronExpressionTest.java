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
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
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

    cronExpression = new CronExpression("0 15 10 L-5W * ? 2010");
    // last day - 5
    cal.set(2010, Calendar.OCTOBER, 26, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

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

    cronExpression = new CronExpression("0 15 10 L-1W,L-1 * ? 2010");
    // nearest weekday to last day - 1 (29th is a friday in 2010)
    cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
    // last day - 1
    cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0);

    cronExpression = new CronExpression("0 15 10 2W,16 * ? 2010");
    // nearest weekday to the 2nd of the month (1st is a friday in 2010)
    cal.set(2010, Calendar.OCTOBER, 1, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 2, 10, 15, 0);
    assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

    cal.set(2010, Calendar.OCTOBER, 16, 10, 15, 0);
    assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

  }

  @Test
  public void testQuartz() throws Exception {
    CronExpression cronExpression = new CronExpression("19 15 10 4 Apr ? ");
    assertEquals("19 15 10 4 Apr ? ".toUpperCase(), cronExpression.getCronExpression());
    assertEquals("19 15 10 4 Apr ? ".toUpperCase(), cronExpression.toString());

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
      assertEquals("Increment > 60 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0/120 0 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 60 : 120", e.getMessage());
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
      assertEquals("Increment > 60 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0/120 8-18 ? * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 60 : 120", e.getMessage());
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
      assertEquals("Increment > 24 : 120", e.getMessage());
    }

    // Test case 2
    try {
      new CronExpression("0 0 0/120 ? * 2-6");
      fail("Cron did not validate bad range interval in in '0/xxx' form");
    } catch (ParseException e) {
      assertEquals("Increment > 24 : 120", e.getMessage());
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

}
