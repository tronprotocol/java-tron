package org.tron.common.backup;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.backup.BackupManager.BackupStatusEnum;
import org.tron.common.backup.message.KeepAliveMessage;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.backup.socket.UdpEvent;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class BackupManagerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private BackupManager manager;
  private BackupServer backupServer;

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[] {"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    manager = new BackupManager();
    backupServer = new BackupServer(manager);
  }

  @After
  public void tearDown() {
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
}
