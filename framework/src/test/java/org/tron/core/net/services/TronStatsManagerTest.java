package org.tron.core.net.services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;
import org.tron.core.net.service.statistics.NodeStatistics;
import org.tron.core.net.service.statistics.TronStatsManager;
import org.tron.protos.Protocol;

public class TronStatsManagerTest {

  @Test
  public void testOnDisconnect() {
    InetSocketAddress inetSocketAddress =
        new InetSocketAddress("127.0.0.2", 10001);

    InetAddress inetAddress = inetSocketAddress.getAddress();

    NodeStatistics statistics = TronStatsManager.getNodeStatistics(inetAddress);

    Assert.assertTrue(null != statistics);
    Assert.assertEquals(Protocol.ReasonCode.UNKNOWN, statistics.getDisconnectReason());
  }

  @Test
  public void testWork() throws Exception {
    TronStatsManager manager = new TronStatsManager();
    Field field1 = manager.getClass().getDeclaredField("TCP_TRAFFIC_IN");
    field1.setAccessible(true);
    field1.set(manager, 1L);

    Field field2 = manager.getClass().getDeclaredField("TCP_TRAFFIC_OUT");
    field2.setAccessible(true);
    field2.set(manager, 1L);

    Field field3 = manager.getClass().getDeclaredField("UDP_TRAFFIC_IN");
    field3.setAccessible(true);
    field3.set(manager, 1L);

    Field field4 = manager.getClass().getDeclaredField("UDP_TRAFFIC_OUT");
    field4.setAccessible(true);
    field4.set(manager, 1L);

    Assert.assertEquals(field1.get(manager), 1L);
    Assert.assertEquals(field2.get(manager), 1L);
    Assert.assertEquals(field3.get(manager), 1L);
    Assert.assertEquals(field4.get(manager), 1L);

    Method method = manager.getClass().getDeclaredMethod("work");
    method.setAccessible(true);
    method.invoke(manager);

    Assert.assertEquals(field1.get(manager), 0L);
    Assert.assertEquals(field2.get(manager), 0L);
    Assert.assertEquals(field3.get(manager), 0L);
    Assert.assertEquals(field4.get(manager), 0L);
  }

}
