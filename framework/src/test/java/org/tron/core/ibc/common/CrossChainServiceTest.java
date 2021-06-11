package org.tron.core.ibc.common;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;


@Slf4j
public class CrossChainServiceTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private ChainBaseManager chainBaseManager;

  private CrossChainService crossChainService;
  private TransactionStore transactionStore;

  @BeforeClass
  public static void init() {
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
  }

  private static String FROMCHAINID = "";
  private static String TOCHAINID = "";

  /**
   * init static var
   */
  static {
    Args.setParam(new String[]{"--output-directory", "dbPath-CrossChainServiceTest"},
        Constant.TEST_CONF);
    FROMCHAINID =
        "00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d11";
    TOCHAINID =
        "00000000000000007adbf8dc20433f587a5f3f8ea83e2877e2129c5128c12d12";
  }

  public static BalanceContract.CrossContract buildCrossContract() {
    BalanceContract.CrossToken.Builder crossToken = BalanceContract.CrossToken.newBuilder();
    crossToken.setAmount(100).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("testCross"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString());

    BalanceContract.CrossContract crossContract = BalanceContract.CrossContract.newBuilder()
        .setOwnerChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(
                "000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        .setToChainId(Sha256Hash.wrap(
            ByteArray.fromHexString(
                "0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
            .getByteString()).setType(BalanceContract.CrossContract.CrossDataType.TOKEN)
        .setData(crossToken.build().toByteString())
        .build();
    return crossContract;
  }

  /**
   * init paras.
   */
  @Before
  public void initParas() {
    chainBaseManager = dbManager.getChainBaseManager();
    crossChainService = context.getBean(CrossChainService.class);
    transactionStore = chainBaseManager.getTransactionStore();
  }

  /**
   * release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
  }


  /**
   * method specific
   */
  @Test
  public void checkCrossChainCommit() {
    BalanceContract.CrossContract crossContract = buildCrossContract();
    TransactionCapsule trx = new TransactionCapsule(crossContract,
        Protocol.Transaction.Contract.ContractType.CrossContract);
    Sha256Hash txId = Sha256Hash.of(true, trx.getInstance().getRawData().toByteArray());

    //txcapsule null check
    boolean nullTxCapsuleResult = crossChainService.checkCrossChainCommit(txId.getByteString());
    Assert.assertFalse(nullTxCapsuleResult);

    //check crossmessage empty
    transactionStore.put(txId.getBytes(), trx);
    boolean emptyCrossMessage = crossChainService.checkCrossChainCommit(txId.getByteString());
    Assert.assertFalse(emptyCrossMessage);

    //not commit check
    boolean blockNotCommitResult = crossChainService.checkCrossChainCommit(txId.getByteString());
    Assert.assertFalse(blockNotCommitResult);

  }


}
