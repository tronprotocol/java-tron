package org.tron.plugins;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Utils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import picocli.CommandLine;

@Slf4j
public class DbLiteTest {

  private TronApplicationContext context;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull;
  private Application appTest;
  private String databaseDir;

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  private String dbPath;
  CommandLine cli = new CommandLine(new DbLite());

  /**
   * init logic.
   */
  public void startApp() {
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.addService(context.getBean(RpcApiService.class));
    appTest.addService(context.getBean(RpcApiServiceOnSolidity.class));
    appTest.startup();

    String fullNode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getRpcPort());
    channelFull = ManagedChannelBuilder.forTarget(fullNode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  /**
   * shutdown the fullNode.
   */
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    context.close();
  }

  public void init() throws IOException {
    dbPath = folder.newFolder().toString();
    Args.setParam(new String[]{"-d", dbPath, "-w", "--p2p-disable", "true"},
        "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    databaseDir = Args.getInstance().getStorage().getDbDirectory();
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
  }

  @After
  public void clear() {
    Args.clearParam();
  }

  void testTools(String dbType, int checkpointVersion)
      throws InterruptedException, IOException {
    logger.info("dbType {}, checkpointVersion {}", dbType, checkpointVersion);
    dbPath = String.format("%s_%s_%d", dbPath, dbType, System.currentTimeMillis());
    init();
    final String[] argsForSnapshot =
        new String[]{"-o", "split", "-t", "snapshot", "--fn-data-path",
            dbPath + File.separator + databaseDir, "--dataset-path",
            dbPath};
    final String[] argsForHistory =
        new String[]{"-o", "split", "-t", "history", "--fn-data-path",
            dbPath + File.separator + databaseDir, "--dataset-path",
            dbPath};
    final String[] argsForMerge =
        new String[]{"-o", "merge", "--fn-data-path", dbPath + File.separator + databaseDir,
            "--dataset-path", dbPath + File.separator + "history"};
    Args.getInstance().getStorage().setDbEngine(dbType);
    Args.getInstance().getStorage().setCheckpointVersion(checkpointVersion);
    DbLite.setRecentBlks(3);
    // start fullNode
    startApp();
    // produce transactions for 18 seconds
    generateSomeTransactions(18);
    // stop the node
    shutdown();
    // delete tran-cache
    FileUtil.deleteDir(Paths.get(dbPath, databaseDir, "trans-cache").toFile());
    // generate snapshot
    cli.execute(argsForSnapshot);
    // start fullNode
    startApp();
    // produce transactions
    generateSomeTransactions(checkpointVersion == 1 ? 6 : 18);
    // stop the node
    shutdown();
    // generate history
    cli.execute(argsForHistory);
    // backup original database to database_bak
    File database = new File(Paths.get(dbPath, databaseDir).toString());
    if (!database.renameTo(new File(Paths.get(dbPath, databaseDir + "_bak").toString()))) {
      throw new RuntimeException(
              String.format("rename %s to %s failed", database.getPath(),
                  Paths.get(dbPath, databaseDir)));
    }
    // change snapshot to the new database
    File snapshot = new File(Paths.get(dbPath, "snapshot").toString());
    if (!snapshot.renameTo(new File(Paths.get(dbPath, databaseDir).toString()))) {
      throw new RuntimeException(
              String.format("rename snapshot to %s failed",
                  Paths.get(dbPath, databaseDir)));
    }
    // start and validate the snapshot
    startApp();
    generateSomeTransactions(checkpointVersion == 1 ? 18 : 6);
    // stop the node
    shutdown();
    // merge history
    cli.execute(argsForMerge);
    // start and validate
    startApp();
    generateSomeTransactions(6);
    shutdown();
    DbLite.reSetRecentBlks();
  }

  private void generateSomeTransactions(int during) {
    during *= 1000; // ms
    int runTime = 0;
    int sleepOnce = 100;
    while (true) {
      ECKey ecKey2 = new ECKey(Utils.getRandom());
      byte[] address = ecKey2.getAddress();

      String sunPri = "cba92a516ea09f620a16ff7ee95ce0df1d56550a8babe9964981a7144c8a784a";
      byte[] sunAddress = PublicMethod.getFinalAddress(sunPri);
      PublicMethod.sendcoin(address, 1L,
              sunAddress, sunPri, blockingStubFull);
      try {
        Thread.sleep(sleepOnce);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if ((runTime += sleepOnce) > during) {
        return;
      }
    }
  }
}
