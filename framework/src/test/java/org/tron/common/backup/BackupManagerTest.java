package org.tron.common.backup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.backup.BackupManager.BackupStatusEnum;
import org.tron.common.backup.message.KeepAliveMessage;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.backup.socket.UdpEvent;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.InetUtil;

public class BackupManagerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private BackupManager manager;
  private BackupServer backupServer;
  private BiFunction<String, Boolean, InetAddress> savedLookup;

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[] {"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    manager = new BackupManager();
    backupServer = new BackupServer(manager);
    savedLookup = InetUtil.dnsLookup;
  }

  @After
  public void tearDown() {
    InetUtil.dnsLookup = savedLookup;
    Args.clearParam();
  }

  @Test
  public void test() throws Exception {
    CommonParameter.getInstance().setBackupPriority(8);
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    CommonParameter.getInstance().setBackupMembers(members);

    Field field = manager.getClass().getDeclaredField("localIp");
    field.setAccessible(true);
    field.set(manager, "127.0.0.1");

    Assert.assertEquals(BackupManager.BackupStatusEnum.MASTER, manager.getStatus());

    field = manager.getClass().getDeclaredField("executorService");
    field.setAccessible(true);
    ScheduledExecutorService executorService = (ScheduledExecutorService) field.get(manager);
    manager.init();
    executorService.shutdown();
    Assert.assertEquals(BackupManager.BackupStatusEnum.INIT, manager.getStatus());

    /* ip not in the members */
    manager.setStatus(BackupManager.BackupStatusEnum.INIT);
    KeepAliveMessage message = new KeepAliveMessage(false, 6);
    InetSocketAddress address = new InetSocketAddress("127.0.0.3", 1000);
    UdpEvent event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupManager.BackupStatusEnum.INIT, manager.getStatus());

    /* ip not the member */
    address = new InetSocketAddress("127.0.0.3", 1000);
    message = new KeepAliveMessage(false, 6);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupManager.BackupStatusEnum.INIT, manager.getStatus());

    /* keepAliveMessage.getFlag() || peerPriority > priority */
    address = new InetSocketAddress("127.0.0.2", 1000);
    message = new KeepAliveMessage(false, 6);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupStatusEnum.SLAVER, manager.getStatus());

    /* keepAliveMessage.getFlag() || peerPriority > priority */
    message = new KeepAliveMessage(false, 10);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupManager.BackupStatusEnum.SLAVER, manager.getStatus());

    /* keepAliveMessage.getFlag() || peerPriority > priority */
    manager.setStatus(BackupManager.BackupStatusEnum.INIT);
    message = new KeepAliveMessage(true, 6);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupManager.BackupStatusEnum.SLAVER, manager.getStatus());

    manager.setStatus(BackupManager.BackupStatusEnum.MASTER);
    message = new KeepAliveMessage(false, 10);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupManager.BackupStatusEnum.MASTER, manager.getStatus());

    message = new KeepAliveMessage(true, 10);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(BackupManager.BackupStatusEnum.SLAVER, manager.getStatus());

  }

  @Test
  public void testSendKeepAliveMessage() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setBackupPriority(8);
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    parameter.setBackupMembers(members);

    Field field = manager.getClass().getDeclaredField("localIp");
    field.setAccessible(true);
    field.set(manager, "127.0.0.1");

    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.MASTER);
    backupServer.initServer();
    manager.init();

    Thread.sleep(parameter.getKeepAliveInterval() + 1000);//test send KeepAliveMessage

    field = manager.getClass().getDeclaredField("executorService");
    field.setAccessible(true);
    ScheduledExecutorService executorService = (ScheduledExecutorService) field.get(manager);
    executorService.shutdown();

    Field field2 = backupServer.getClass().getDeclaredField("executor");
    field2.setAccessible(true);
    ExecutorService executorService2 = (ExecutorService) field2.get(backupServer);
    executorService2.shutdown();

    Assert.assertEquals(BackupManager.BackupStatusEnum.INIT, manager.getStatus());
  }

  // ===== domain-handling tests for init() =====

  @Test(timeout = 5000)
  public void testInitResolvesDomainsToMembers() throws Exception {
    CommonParameter.getInstance().setBackupMembers(
        Collections.singletonList("node.example.com"));
    InetAddress resolved = InetAddress.getByName("1.2.3.4");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("node.example.com".equals(host) && ipv4) ? resolved : null;
    manager.init();
    Set<String> members = getField(manager, "members");
    Map<String, String> cache = getField(manager, "domainIpCache");
    Assert.assertTrue(members.contains("1.2.3.4"));
    Assert.assertEquals("1.2.3.4", cache.get("node.example.com"));
    manager.stop();
  }

  @Test(timeout = 5000)
  public void testInitSkipsUnresolvableDomain() throws Exception {
    CommonParameter.getInstance().setBackupMembers(
        Collections.singletonList("bad.invalid.domain"));
    InetUtil.dnsLookup = (host, ipv4) -> null;
    manager.init();
    Set<String> members = getField(manager, "members");
    Map<String, String> cache = getField(manager, "domainIpCache");
    Assert.assertTrue("unresolvable domain should be silently dropped", members.isEmpty());
    Assert.assertTrue(cache.isEmpty());
    manager.stop();
  }

  @Test(timeout = 5000)
  public void testInitSkipsDomainResolvingToLocalIp() throws Exception {
    String localIp = InetAddress.getLocalHost().getHostAddress();
    CommonParameter.getInstance().setBackupMembers(
        Collections.singletonList("self.local.host"));
    InetAddress selfAddr = InetAddress.getByName(localIp);
    InetUtil.dnsLookup = (host, ipv4) ->
        ("self.local.host".equals(host) && ipv4) ? selfAddr : null;
    manager.init();
    Set<String> members = getField(manager, "members");
    Assert.assertFalse("domain resolving to local IP should not be in members",
        members.contains(localIp));
    manager.stop();
  }

  // ===== refreshMemberIps() tests =====

  @Test(timeout = 5000)
  public void testRefreshMemberIpsIpChanged() throws Exception {
    Set<String> members = getField(manager, "members");
    Map<String, String> cache = getField(manager, "domainIpCache");
    members.add("1.1.1.1");
    cache.put("peer.tron.network", "1.1.1.1");

    InetAddress newAddr = InetAddress.getByName("2.2.2.2");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("peer.tron.network".equals(host) && ipv4) ? newAddr : null;
    invokeRefreshMemberIps(manager);
    Assert.assertFalse(members.contains("1.1.1.1"));
    Assert.assertTrue(members.contains("2.2.2.2"));
    Assert.assertEquals("2.2.2.2", cache.get("peer.tron.network"));
  }

  @Test(timeout = 5000)
  public void testRefreshMemberIpsIpUnchanged() throws Exception {
    Set<String> members = getField(manager, "members");
    Map<String, String> cache = getField(manager, "domainIpCache");
    members.add("1.1.1.1");
    cache.put("peer.tron.network", "1.1.1.1");

    InetAddress sameAddr = InetAddress.getByName("1.1.1.1");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("peer.tron.network".equals(host) && ipv4) ? sameAddr : null;
    invokeRefreshMemberIps(manager);
    Assert.assertTrue(members.contains("1.1.1.1"));
    Assert.assertEquals("1.1.1.1", cache.get("peer.tron.network"));
  }

  @Test(timeout = 5000)
  public void testRefreshMemberIpsDnsFailure() throws Exception {
    Set<String> members = getField(manager, "members");
    Map<String, String> cache = getField(manager, "domainIpCache");
    members.add("1.1.1.1");
    cache.put("peer.tron.network", "1.1.1.1");

    InetUtil.dnsLookup = (host, ipv4) -> null;
    invokeRefreshMemberIps(manager);
    Assert.assertTrue("old IP should be kept on DNS failure", members.contains("1.1.1.1"));
    Assert.assertEquals("1.1.1.1", cache.get("peer.tron.network"));
  }

  @SuppressWarnings("unchecked")
  private <T> T getField(Object obj, String name) throws Exception {
    Field f = obj.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return (T) f.get(obj);
  }

  private void invokeRefreshMemberIps(BackupManager mgr) throws Exception {
    Method m = mgr.getClass().getDeclaredMethod("refreshMemberIps");
    m.setAccessible(true);
    m.invoke(mgr);
  }
}
