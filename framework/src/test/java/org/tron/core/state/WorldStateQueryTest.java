package org.tron.core.state;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.WalletUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract;

public class WorldStateQueryTest {
  private static TronApplicationContext context;
  private static Application appTest;
  private static ChainBaseManager chainBaseManager;
  private static Manager manager;

  private static String dbPath = "output-directory-state";

  private ECKey account1Prikey = ECKey.fromPrivate( // TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW
      ByteArray.fromHexString("D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366"));
  private ECKey account2Prikey = ECKey.fromPrivate( // TGehVcNhud84JDCGrNHKVz9jEAVKUpbuiv
      ByteArray.fromHexString("cba92a516ea09f620a16ff7ee95ce0df1d56550a8babe9964981a7144c8a784a"));

  byte[] contractAddress;

  /**
   * init logic.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.addService(context.getBean(RpcApiService.class));
    appTest.addService(context.getBean(RpcApiServiceOnSolidity.class));
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();
    chainBaseManager = context.getBean(ChainBaseManager.class);
    manager = context.getBean(Manager.class);
  }

  @AfterClass
  public static void destory() {
    appTest.shutdown();
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testTransfer() throws InterruptedException {
    manager.initGenesis();
    List<BlockCapsule> blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    BlockCapsule blockCapsule = blockCapsules.get(0);
    try {
      manager.pushBlock(buildTransferBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);
    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    byte[] rootHash = blockCapsule.getArchiveStateRoot().getBytes();
    WorldStateQueryInstance worldStateQueryInstance = new WorldStateQueryInstance(rootHash, chainBaseManager);
    checkAccount(worldStateQueryInstance);

    try {
      manager.pushBlock(buildTransferBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);

    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    rootHash = blockCapsule.getArchiveStateRoot().getBytes();
    worldStateQueryInstance = new WorldStateQueryInstance(rootHash, chainBaseManager);
    checkAccount(worldStateQueryInstance);
  }

  @Test
  public void testContract() throws InterruptedException {
    manager.initGenesis();
    List<BlockCapsule> blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    BlockCapsule blockCapsule = blockCapsules.get(0);
    try {
      manager.pushBlock(buildContractBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);
    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    byte[] rootHash = blockCapsule.getArchiveStateRoot().getBytes();
    WorldStateQueryInstance worldStateQueryInstance = new WorldStateQueryInstance(rootHash, chainBaseManager);
    Assert.assertArrayEquals(chainBaseManager.getContractStore().get(contractAddress).getData(),
        worldStateQueryInstance.getContract(contractAddress).getData());
    checkAccount(worldStateQueryInstance);

    try {
      manager.pushBlock(buildTriggerBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);

    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    rootHash = blockCapsule.getArchiveStateRoot().getBytes();
    worldStateQueryInstance = new WorldStateQueryInstance(rootHash, chainBaseManager);
    ContractCapsule contractCapsule = worldStateQueryInstance.getContract(contractAddress);

    Storage storage = new Storage(contractAddress, chainBaseManager.getStorageRowStore());
    storage.setContractVersion(contractCapsule.getInstance().getVersion());

    DataWord value1 = storage.getValue(new DataWord(ByteArray.fromHexString("0")),
        worldStateQueryInstance);

    DataWord value2 = storage.getValue(new DataWord(ByteArray.fromHexString("0")));
    Assert.assertArrayEquals(value1.getData(), value2.getData());
    checkAccount(worldStateQueryInstance);
  }

  private void checkAccount(WorldStateQueryInstance worldStateQueryInstance) {
    AccountCapsule account1Capsule = chainBaseManager.getAccountStore().get(account1Prikey.getAddress());
    account1Capsule.clearAsset();
    AccountCapsule account2Capsule = chainBaseManager.getAccountStore().get(account2Prikey.getAddress());
    account2Capsule.clearAsset();
    Assert.assertArrayEquals(account1Capsule.getInstance().toByteArray(),
        worldStateQueryInstance.getAccount(account1Prikey.getAddress()).getInstance().toByteArray());
    Assert.assertArrayEquals(account2Capsule.getInstance().toByteArray(),
        worldStateQueryInstance.getAccount(account2Prikey.getAddress()).getInstance().toByteArray());
  }

  private BlockCapsule buildTransferBlock(BlockCapsule parentBlock) {
    BalanceContract.TransferContract transferContract = BalanceContract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(account1Prikey.getAddress()))
        .setToAddress(ByteString.copyFrom(account2Prikey.getAddress()))
        .setAmount(1).build();

    Protocol.Transaction.raw.Builder transactionBuilder = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder().setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder()
        .setRawData(transactionBuilder.build()).build();

    TransactionCapsule transactionCapsule = setAndSignTx(transaction, parentBlock, account1Prikey);

    BlockCapsule blockCapsule =
        new BlockCapsule(
            parentBlock.getNum()+1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()),
            System.currentTimeMillis(),
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    org.tron.common.utils.ByteArray.fromHexString(
                        Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    return blockCapsule;
  }

  private BlockCapsule buildContractBlock(BlockCapsule parentBlock) {
    long value = 0L;
    long feeLimit = 1_000_000_000L;
    long consumeUserResourcePercent = 0L;
    String contractName = "increment";
    String ABI = "[]";
    String code = "60806040526000805534801561001457600080fd5b50610181806100246000396000f3fe608060" +
        "405234801561001057600080fd5b50600436106100415760003560e01c806342cbb15c146100465780636d4c" +
        "e63c14610064578063d09de08a14610082575b600080fd5b61004e61008c565b60405161005b91906100cd56" +
        "5b60405180910390f35b61006c610094565b60405161007991906100cd565b60405180910390f35b61008a61" +
        "009d565b005b600043905090565b60008054905090565b60016000546100ac9190610117565b600081905550" +
        "565b6000819050919050565b6100c7816100b4565b82525050565b60006020820190506100e2600083018461" +
        "00be565b92915050565b7f4e487b710000000000000000000000000000000000000000000000000000000060" +
        "0052601160045260246000fd5b6000610122826100b4565b915061012d836100b4565b925082820190508082" +
        "1115610145576101446100e8565b5b9291505056fea26469706673582212207c5e242c88722ac1f7f5f1ea67" +
        "0cf1a784cad42b058651ceaf6fe0fc10ebff8264736f6c63430008110033";
    String libraryAddressPair = null;
    Protocol.Transaction transaction = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, account1Prikey.getAddress(), ABI, code, value, feeLimit,
        consumeUserResourcePercent, libraryAddressPair);
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setResultCode(Protocol.Transaction.Result.contractResult.SUCCESS);

    transactionCapsule = setAndSignTx(transactionCapsule.getInstance(), parentBlock, account1Prikey);


    BlockCapsule blockCapsule =
        new BlockCapsule(
            parentBlock.getNum()+1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()),
            System.currentTimeMillis(),
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    org.tron.common.utils.ByteArray.fromHexString(
                        Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    contractAddress = WalletUtil.generateContractAddress(transactionCapsule.getInstance());
    return blockCapsule;
  }

  private BlockCapsule buildTriggerBlock(BlockCapsule parentBlock) {
    long value = 0L;
    long feeLimit = 1_000_000_000L;
    byte[] triggerData = TvmTestUtils.parseAbi("increment()", null);
    Protocol.Transaction transaction = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
        account1Prikey.getAddress(), contractAddress, triggerData, value, feeLimit);

    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setResultCode(Protocol.Transaction.Result.contractResult.SUCCESS);

    transactionCapsule = setAndSignTx(transactionCapsule.getInstance(), parentBlock, account1Prikey);

    BlockCapsule blockCapsule =
        new BlockCapsule(
            parentBlock.getNum()+1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()),
            System.currentTimeMillis(),
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    org.tron.common.utils.ByteArray.fromHexString(
                        Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    return blockCapsule;
  }

  private TransactionCapsule setAndSignTx(Protocol.Transaction transaction, BlockCapsule parentBlock, ECKey account) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setReference(parentBlock.getNum(), parentBlock.getBlockId().getBytes());
    transactionCapsule.setExpiration(parentBlock.getTimeStamp() + 60*60*1000);
    return new TransactionCapsule(PublicMethod.signTransaction(account, transactionCapsule.getInstance()));
  }

}
