package org.tron.program;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.tool.litefullnode.LiteFullNodeTool;
import stest.tron.wallet.common.client.utils.TransactionUtils;

public class LiteFullNodeToolTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  private TronApplicationContext context;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private Application appTest;

  private String databaseDir;

  @Rule
  public ExpectedException thrown = ExpectedException.none();


  private static final String DB_PATH = "output_lite_fn";

  /**
   * init logic.
   */
  public void startApp() {
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.addService(context.getBean(RpcApiService.class));
    appTest.addService(context.getBean(RpcApiServiceOnSolidity.class));
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();

    String fullnode = String.format("%s:%d", "127.0.0.1",
            Args.getInstance().getRpcPort());
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  /**
   *  Delete the database when exit.
   */
  public static void destroy(String dbPath) {
    File f = new File(dbPath);
    if (f.exists()) {
      if (FileUtil.deleteDir(f)) {
        logger.info("Release resources successful.");
      } else {
        logger.info("Release resources failure.");
      }
    }
  }

  /**
   * shutdown the fullnode.
   */
  public void shutdown() {
    appTest.shutdownServices();
    appTest.shutdown();
    context.destroy();
  }

  @Before
  public void init() {
    destroy(DB_PATH); // delete if prev failed
    Args.setParam(new String[]{"-d", DB_PATH, "-w"}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    databaseDir = Args.getInstance().getStorage().getDbDirectory();
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
  }

  @After
  public void clear() {
    destroy(DB_PATH);
    Args.clearParam();
  }

  @Test
  public void testToolsWithLevelDB() {
    logger.info("testToolsWithLevelDB start");
    testTools("LEVELDB", 1);
  }

  @Test
  public void testToolsWithLevelDBV2() {
    logger.info("testToolsWithLevelDB start");
    testTools("LEVELDB", 2);
  }

  @Test
  public void testToolsWithRocksDB() {
    logger.info("testToolsWithRocksDB start");
    testTools("ROCKSDB", 1);
  }

  private void testTools(String dbType, int checkpointVersion) {
    final String[] argsForSnapshot =
        new String[]{"-o", "split", "-t", "snapshot", "--fn-data-path",
            DB_PATH + File.separator + databaseDir, "--dataset-path",
            DB_PATH};
    final String[] argsForHistory =
        new String[]{"-o", "split", "-t", "history", "--fn-data-path",
            DB_PATH + File.separator + databaseDir, "--dataset-path",
            DB_PATH};
    final String[] argsForMerge =
        new String[]{"-o", "merge", "--fn-data-path", DB_PATH + File.separator + databaseDir,
            "--dataset-path", DB_PATH + File.separator + "history"};
    Args.getInstance().getStorage().setDbEngine(dbType);
    Args.getInstance().getStorage().setCheckpointVersion(checkpointVersion);
    LiteFullNodeTool.setRecentBlks(3);
    // start fullnode
    startApp();
    // produce transactions for 18 seconds
    generateSomeTransactions(18);
    // stop the node
    shutdown();
    // delete tran-cache
    FileUtil.deleteDir(Paths.get(DB_PATH, databaseDir, "trans-cache").toFile());
    // generate snapshot
    LiteFullNodeTool.main(argsForSnapshot);
    // start fullnode
    startApp();
    // produce transactions for 6 seconds
    generateSomeTransactions(6);
    // stop the node
    shutdown();
    // generate history
    LiteFullNodeTool.main(argsForHistory);
    // backup original database to database_bak
    File database = new File(Paths.get(DB_PATH, databaseDir).toString());
    if (!database.renameTo(new File(Paths.get(DB_PATH, databaseDir + "_bak").toString()))) {
      throw new RuntimeException(
              String.format("rename %s to %s failed", database.getPath(),
                      Paths.get(DB_PATH, databaseDir).toString()));
    }
    // change snapshot to the new database
    File snapshot = new File(Paths.get(DB_PATH, "snapshot").toString());
    if (!snapshot.renameTo(new File(Paths.get(DB_PATH, databaseDir).toString()))) {
      throw new RuntimeException(
              String.format("rename snapshot to %s failed",
                      Paths.get(DB_PATH, databaseDir).toString()));
    }
    // start and validate the snapshot
    startApp();
    generateSomeTransactions(6);
    // stop the node
    shutdown();
    // merge history
    LiteFullNodeTool.main(argsForMerge);
    // start and validate
    startApp();
    generateSomeTransactions(6);
    shutdown();
    LiteFullNodeTool.reSetRecentBlks();
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
