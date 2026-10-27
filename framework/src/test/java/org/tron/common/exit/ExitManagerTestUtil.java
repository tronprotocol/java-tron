package org.tron.common.exit;

import static org.mockito.Mockito.mockStatic;

import org.mockito.MockedStatic;

public final class ExitManagerTestUtil {

  private ExitManagerTestUtil() {
  }

  public static MockedStatic<ExitManager> suppressExit() {
    return mockStatic(ExitManager.class, invocation ->
        "logAndExit".equals(invocation.getMethod().getName())
            ? null : invocation.callRealMethod());
  }
}
