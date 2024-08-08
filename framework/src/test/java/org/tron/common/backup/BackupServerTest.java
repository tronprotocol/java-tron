package org.tron.common.backup;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;


public class BackupServerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private BackupServer backupServer;

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(80);
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    CommonParameter.getInstance().setBackupMembers(members);
    BackupManager backupManager = new BackupManager();
    backupManager.init();
    backupServer = new BackupServer(backupManager);
  }

  @After
  public void tearDown() {
    backupServer.close();
    Args.clearParam();
  }

  @Test(timeout = 60_000)
  public void test() throws InterruptedException {
    backupServer.initServer();
    // wait for the server to start
    Thread.sleep(1000);
  }
}
