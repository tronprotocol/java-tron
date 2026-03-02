package org.tron.common.utils;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.Before;
import org.junit.Test;

public class ALockTest {
  private ALock aLock;
  private Lock mockLock;

  @Before
  public void setUp() {
    mockLock = new ReentrantLock();
    aLock = new ALock(mockLock);
  }

  @Test
  public void testLockAndUnlock() {
    aLock.lock();
    assertNotNull(aLock);
    aLock.close();
  }

}
