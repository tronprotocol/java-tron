package org.tron.common.backup;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.backup.message.KeepAliveMessage;
import org.tron.common.backup.socket.UdpEvent;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class BackupManagerTest {

  @Test
  public void test() throws Exception {
    String[] a = new String[0];
    Args.setParam(a, Constant.TESTNET_CONF);
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setBackupPriority(8);
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    parameter.setBackupMembers(members);

    BackupManager manager = new BackupManager();

    Field field =  manager.getClass().getDeclaredField("localIp");
    field.setAccessible(true);
    field.set(manager, "127.0.0.1");

    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.MASTER);

    field =  manager.getClass().getDeclaredField("executorService");
    field.setAccessible(true);
    ScheduledExecutorService executorService = (ScheduledExecutorService) field.get(manager);
    manager.init();
    executorService.shutdown();
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.INIT);

    /* ip not in the members */
    manager.setStatus(BackupManager.BackupStatusEnum.INIT);
    KeepAliveMessage message = new KeepAliveMessage(false, 6);
    InetSocketAddress address = new InetSocketAddress("127.0.0.3", 1000);
    UdpEvent event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.INIT);

    /* ip not the member */
    address = new InetSocketAddress("127.0.0.3", 1000);
    message = new KeepAliveMessage(false, 6);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.INIT);

    /* keepAliveMessage.getFlag() || peerPriority > priority */
    address = new InetSocketAddress("127.0.0.2", 1000);
    message = new KeepAliveMessage(false, 6);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.INIT);

    /* keepAliveMessage.getFlag() || peerPriority > priority */
    message = new KeepAliveMessage(false, 10);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.SLAVER);

    /* keepAliveMessage.getFlag() || peerPriority > priority */
    manager.setStatus(BackupManager.BackupStatusEnum.INIT);
    message = new KeepAliveMessage(true, 6);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.SLAVER);

    manager.setStatus(BackupManager.BackupStatusEnum.MASTER);
    message = new KeepAliveMessage(false, 10);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.MASTER);

    message = new KeepAliveMessage(true, 10);
    event = new UdpEvent(message, address);
    manager.handleEvent(event);
    Assert.assertEquals(manager.getStatus(), BackupManager.BackupStatusEnum.SLAVER);

  }
}
