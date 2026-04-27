package org.tron.common.backup;

import org.junit.After;
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

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    BackupManager backupManager = new BackupManager();
    backupManager.init();
    backupServer = new BackupServer(backupManager);
  }

  @After
  public void tearDown() {
    backupServer.close();
    Args.clearParam();
  }

  @Test
  public void test() {
    // backupMembers is empty so initServer() is a no-op; verify close() is safe
    backupServer.initServer();
  }
}
