package org.tron.common.backup;

import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.tron.common.TestConstants;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;


public class BackupServerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Timeout globalTimeout = Timeout.seconds(60);
  private BackupServer backupServer;
  private BackupManager backupManager;
  private boolean backupServerClosed;

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    CommonParameter.getInstance().setBackupMembers(members);
    backupManager = new BackupManager();
    backupManager.init();
    backupServer = new BackupServer(backupManager);
  }

  @After
  public void tearDown() throws Exception {
    List<Throwable> errors = new ArrayList<>();
    if (!backupServerClosed && backupServer != null) {
      BackupTestUtils.runQuietly(errors, backupServer::close);
    }
    if (backupManager != null) {
      BackupTestUtils.runQuietly(errors, backupManager::stop);
    }
    BackupTestUtils.runQuietly(errors,
        () -> BackupTestUtils.assertExecutorsTerminated(backupManager, backupServer));
    Args.clearParam();
    BackupTestUtils.throwIfAnyError(errors);
  }

  @Test(timeout = 60_000)
  public void test() throws Exception {
    backupServer.initServer();
    BackupTestUtils.awaitCondition("backup channel to become active",
        () -> BackupTestUtils.getChannel(backupServer) != null
            && BackupTestUtils.getChannel(backupServer).isActive());
    Channel channel = BackupTestUtils.getChannel(backupServer);
    Assert.assertTrue("backup channel must be active after startup", channel.isActive());

    backupServer.close();
    backupServerClosed = true;

    Assert.assertFalse("backup channel must close", channel.isOpen());
    BackupTestUtils.assertExecutorsTerminated(backupManager, backupServer);
  }
}
