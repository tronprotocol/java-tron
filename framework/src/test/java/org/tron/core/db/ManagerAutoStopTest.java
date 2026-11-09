package org.tron.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.tron.common.TestConstants;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.cron.CronExpression;
import org.tron.common.exit.ExitManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;

public class ManagerAutoStopTest {

  private static final String PREVIEW_PREFIX = "Auto-stop: shutDownBlockTime ";
  private static final String CANDIDATE_NOTE =
      "The target is the first block at or after the first match.";
  private static final long HEAD_TIME = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli();
  private static final String HEAD_TEXT = "Mon 2026-06-01 00:00:00 UTC";

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final Logger dbLogger = (Logger) LoggerFactory.getLogger("DB");
  private final ListAppender<ILoggingEvent> dbLog = new ListAppender<>();
  private Manager startupManager;

  @Before
  public void attachLog() {
    dbLog.start();
    dbLogger.addAppender(dbLog);
  }

  @After
  public void cleanUp() {
    dbLogger.detachAppender(dbLog);
    Mockito.framework().clearInlineMocks();
    if (startupManager != null) {
      startupManager.close();
    }
    Args.clearParam();
  }

  @Test
  public void singleFutureMatchIsPreviewedAndNotRejected() throws Exception {
    CronExpression cron = utcCron("0 0 12 15 6 ? 2027");

    runInitAutoStop(HEAD_TIME, 0L, cron);

    assertEquals(Collections.singletonList(preview("0 0 12 15 6 ? 2027", HEAD_TEXT,
        "Tue 2027-06-15 12:00:00 UTC")), previews());
    verify(cron, times(2)).getNextValidTimeAfter(any(Date.class));
  }

  @Test
  public void twoFutureMatchesEndThePreviewAtNull() throws Exception {
    CronExpression cron = utcCron("0 0 12 15 6 ? 2027-2028");

    runInitAutoStop(HEAD_TIME, 0L, cron);

    assertEquals(Collections.singletonList(preview("0 0 12 15 6 ? 2027-2028", HEAD_TEXT,
        "Tue 2027-06-15 12:00:00 UTC", "Thu 2028-06-15 12:00:00 UTC")), previews());
    verify(cron, times(3)).getNextValidTimeAfter(any(Date.class));
  }

  @Test
  public void thirdMatchEndsThePreviewWithoutAFourthQuery() throws Exception {
    CronExpression cron = utcCron("0 0 12 15 6 ? 2027-2029");

    runInitAutoStop(HEAD_TIME, 0L, cron);

    assertEquals(Collections.singletonList(preview("0 0 12 15 6 ? 2027-2029", HEAD_TEXT,
        "Tue 2027-06-15 12:00:00 UTC", "Thu 2028-06-15 12:00:00 UTC",
        "Fri 2029-06-15 12:00:00 UTC")), previews());
    verify(cron, times(3)).getNextValidTimeAfter(any(Date.class));
  }

  @Test
  public void moreThanThreeFutureMatchesPreviewOnlyThree() throws Exception {
    CronExpression cron = utcCron("0 0 12 * * ?");

    runInitAutoStop(HEAD_TIME, 0L, cron);

    assertEquals(Collections.singletonList(preview("0 0 12 * * ?", HEAD_TEXT,
        "Mon 2026-06-01 12:00:00 UTC", "Tue 2026-06-02 12:00:00 UTC",
        "Wed 2026-06-03 12:00:00 UTC")), previews());
    verify(cron, times(3)).getNextValidTimeAfter(any(Date.class));
  }

  @Test
  public void noFutureMatchKeepsTheStartupRejection() throws Exception {
    CronExpression cron = utcCron("0 0 12 1 1 ? 2020");
    long head = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli();

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> runInitAutoStop(head, 0L, cron));

    assertEquals("shutDownBlockTime 0 0 12 1 1 ? 2020 is illegal", thrown.getMessage());
    assertTrue(previews().isEmpty());
    verify(cron, times(1)).getNextValidTimeAfter(any(Date.class));
  }

  @Test
  public void previewStartsFromTheHeadBlockTime() throws Exception {
    CronExpression cron = utcCron("0 0 12 1 1 ? 2001");
    long head = Instant.parse("2000-01-01T00:00:00Z").toEpochMilli();

    runInitAutoStop(head, 0L, cron);

    assertEquals(Collections.singletonList(preview("0 0 12 1 1 ? 2001",
        "Sat 2000-01-01 00:00:00 UTC", "Mon 2001-01-01 12:00:00 UTC")), previews());
    verify(cron).getNextValidTimeAfter(new Date(head));
  }

  @Test
  public void previewIgnoresTheDefaultZoneAndLocale() throws Exception {
    TimeZone defaultZone = TimeZone.getDefault();
    Locale defaultLocale = Locale.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    Locale.setDefault(new Locale("th", "TH"));
    try {
      runInitAutoStop(HEAD_TIME, 0L, utcCron("0 0 12 15 6 ? 2027"));
    } finally {
      TimeZone.setDefault(defaultZone);
      Locale.setDefault(defaultLocale);
    }

    assertEquals(Collections.singletonList(preview("0 0 12 15 6 ? 2027", HEAD_TEXT,
        "Tue 2027-06-15 12:00:00 UTC")), previews());
  }

  @Test
  public void withoutBlockTimeNothingIsPreviewed() throws Exception {
    Manager dbManager = runInitAutoStop(HEAD_TIME, 100L, null);

    assertTrue(previews().isEmpty());
    assertEquals(100L, (long) ReflectUtils.getFieldValue(dbManager, "latestSolidityNumShutDown"));
  }

  @Test
  public void cronRuntimeFailureStopsStartupWithAutoStopParams() throws Exception {
    Args.setParam(new String[] {"--output-directory", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    Args.getInstance().setShutdownBlockTime(new CronExpression("0 0 0 1 L ?"));
    TronApplicationContext context = new TronApplicationContext();
    context.getDefaultListableBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof Manager) {
          startupManager = (Manager) bean;
        }
        return bean;
      }
    });
    context.register(DefaultConfig.class);

    BeansException thrown = assertThrows(BeansException.class, context::refresh);

    TronError error = ExitManager.findTronError(thrown).orElseThrow(AssertionError::new);
    assertEquals(TronError.ErrCode.AUTO_STOP_PARAMS, error.getErrCode());
    assertEquals(NoSuchElementException.class, error.getCause().getClass());
    assertTrue(dbLog.list.stream().map(ILoggingEvent::getFormattedMessage).anyMatch(
        "Auto-stop params error: java.util.NoSuchElementException"::equals));
  }

  private CronExpression utcCron(String expression) throws ParseException {
    CronExpression cron = new CronExpression(expression);
    cron.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    return spy(cron);
  }

  private Manager runInitAutoStop(long headTime, long exitHeight, CronExpression blockTime) {
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    when(chainBaseManager.getHeadBlockNum()).thenReturn(10L);
    when(chainBaseManager.getHeadBlockTimeStamp()).thenReturn(headTime);
    Manager dbManager = new Manager();
    ReflectUtils.setFieldValue(dbManager, "chainBaseManager", chainBaseManager);
    CommonParameter parameter = new CommonParameter();
    parameter.setShutdownBlockHeight(exitHeight);
    parameter.setShutdownBlockCount(-1L);
    parameter.setShutdownBlockTime(blockTime);
    try (MockedStatic<CommonParameter> ignored = mockStatic(CommonParameter.class)) {
      when(CommonParameter.getInstance()).thenReturn(parameter);
      ReflectUtils.invokeMethod(dbManager, "initAutoStop");
    }
    return dbManager;
  }

  private List<String> previews() {
    return dbLog.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .filter(message -> message.startsWith(PREVIEW_PREFIX))
        .collect(Collectors.toList());
  }

  private static String preview(String expression, String head, String... matches) {
    return PREVIEW_PREFIX + expression + ", head block time " + head + ", next cron matches "
        + Arrays.toString(matches) + ". " + CANDIDATE_NOTE;
  }
}
