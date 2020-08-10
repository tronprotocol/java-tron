package org.tron.program;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.DatabaseGrpc;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.FileUtil;
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
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private DatabaseGrpc.DatabaseBlockingStub databaseBlockingStub = null;
  private RpcApiService rpcApiService;
  private RpcApiServiceOnSolidity rpcApiServiceOnSolidity;
  private Application appTest;

  private String databaseDir;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * init logic.
   */
  public void startApp() {
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    rpcApiService = context.getBean(RpcApiService.class);
    rpcApiServiceOnSolidity = context.getBean(RpcApiServiceOnSolidity.class);
    appTest.addService(rpcApiService);
    appTest.addService(rpcApiServiceOnSolidity);
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();

    String fullnode = String.format("%s:%d", "127.0.0.1",
            Args.getInstance().getRpcPort());
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelFull);
    databaseBlockingStub = DatabaseGrpc.newBlockingStub(channelFull);
  }

  /**
   *  Delete the database when exit.
   */
  public static void destory(String dbPath) {
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
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

  @Test
  public void testToolsWithLevelDB() {
    String dbPath = "output_lite_fn_leveldb_test";
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    databaseDir = Args.getInstance().getStorage().getDbDirectory();
    testTools("LEVELDB", dbPath);
    destory(dbPath);
  }

  @Test
  public void testToolsWithRocksDB() {
    String dbPath = "output_lite_fn_rocksdb_test";
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    databaseDir = Args.getInstance().getStorage().getDbDirectory();
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
    testTools("ROCKSDB", dbPath);
    destory(dbPath);
  }

  private void testTools(String dbType, String dbPath) {
    final String[] argsForSnapshot =
        new String[]{"-o", "split", "-t", "snapshot", "--fn-data-path",
            dbPath + File.separator + databaseDir, "--dataset-path", dbPath};
    final String[] argsForHistory =
        new String[]{"-o", "split", "-t", "history", "--fn-data-path",
            dbPath + File.separator + databaseDir, "--dataset-path", dbPath};
    final String[] argsForMerge =
        new String[]{"-o", "merge", "--fn-data-path", dbPath + File.separator + databaseDir,
            "--dataset-path", dbPath + File.separator + "history"};
    Args.getInstance().getStorage().setDbEngine(dbType);
    // start fullnode
    startApp();
    // produce transactions for 10 seconds
    generateSomeTransactions(10);
    // stop the node
    shutdown();
    // delete tran-cache
    FileUtil.deleteDir(Paths.get(dbPath, databaseDir, "trans-cache").toFile());
    // generate snapshot
    LiteFullNodeTool.main(argsForSnapshot);
    // start fullnode
    startApp();
    // produce transactions for 10 seconds
    generateSomeTransactions(4);
    // stop the node
    shutdown();
    // generate history
    LiteFullNodeTool.main(argsForHistory);
    // backup original database to database_bak
    File database = new File(Paths.get(dbPath, databaseDir).toString());
    if (!database.renameTo(new File(Paths.get(dbPath, databaseDir + "_bak").toString()))) {
      throw new RuntimeException(
              String.format("rename %s to %s failed", database.getPath(),
                      Paths.get(dbPath, databaseDir).toString()));
    }
    // change snapshot to the new database
    File snapshot = new File(Paths.get(dbPath, "snapshot").toString());
    if (!snapshot.renameTo(new File(Paths.get(dbPath, databaseDir).toString()))) {
      throw new RuntimeException(
              String.format("rename snapshot to %s failed",
                      Paths.get(dbPath, databaseDir).toString()));
    }
    // start and validate the snapshot
    startApp();
    generateSomeTransactions(4);
    // stop the node
    shutdown();
    // merge history
    LiteFullNodeTool.main(argsForMerge);
    // start and validate
    startApp();
    generateSomeTransactions(4);
    shutdown();
  }

  private void generateSomeTransactions(int during) {
    during *= 1000; // ms
    int runTime = 0;
    int sleepOnce = 100;
    while (true) {
      ECKey ecKey2 = new ECKey(Utils.getRandom());
      byte[] address = ecKey2.getAddress();

      String sunPri = "cba92a516ea09f620a16ff7ee95ce0df1d56550a8babe9964981a7144c8a784a";
      byte[] sunAddress = getFinalAddress(sunPri);
      sendcoin(address, 1L,
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

  /**
   * Set public for future use.
   * @param priKey private key
   * @return public addr
   */
  public static byte[] getFinalAddress(String priKey) {
    Wallet.setAddressPreFixByte((byte) 0x41);
    ECKey key = ECKey.fromPrivate(new BigInteger(priKey, 16));
    return key.getAddress();
  }

  /**
   * Set public for future use.
   * @param to addr receives the asset
   * @param amount asset amount
   * @param owner  sender
   * @param priKey private key of the sender
   * @param blockingStubFull Grpc interface
   * @return true or false
   */
  public static Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey,
                                 WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte((byte) 0x41);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    int times = 0;
    while (times++ <= 2) {

      BalanceContract.TransferContract.Builder builder =
              BalanceContract.TransferContract.newBuilder();
      ByteString bsTo = ByteString.copyFrom(to);
      ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      BalanceContract.TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        continue;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /**
   * Set public for future use.
   * @param ecKey ecKey of the private key
   * @param transaction transaction object
   */
  public static Protocol.Transaction signTransaction(ECKey ecKey,
                                                     Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * Set public for future use.
   * @param transaction transaction object
   * @param blockingStubFull Grpc interface
   */
  public static GrpcAPI.Return broadcastTransaction(
          Protocol.Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    while (!response.getResult() && response.getCode() == GrpcAPI.Return.response_code.SERVER_BUSY
            && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
    }
    return response;
  }

}
