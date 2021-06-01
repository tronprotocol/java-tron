package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;


@Slf4j
public class CrossChainActuatorTest {

  private static final String dbPath = "output_crosschain_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_CHAINID;
  private static final String TO_ADDRESS;
  private static final String TO_CHAINID;
  private static TronApplicationContext context;
  private static TransactionCapsule trx;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TO_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_CHAINID =
        "00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d1e";
    TO_CHAINID =
        "000000000000000029b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c";
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder()
            .setRawData(Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder()
            .setType(Protocol.Transaction.Contract.ContractType.CrossContract))).build();
    trx = new TransactionCapsule(transaction);
    trx.setType(Protocol.CrossMessage.Type.DATA);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void initTest() {
    dbManager.getDynamicPropertiesStore().saveCrossChain(1);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
  }

  private BalanceContract.CrossToken getCrossContractData() {
    BalanceContract.CrossToken.Builder crossToken = BalanceContract.CrossToken.newBuilder();
    crossToken.setAmount(100).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("1000001"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000029b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString());
    return crossToken.build();
  }

  private Any getContract() {
    return Any.pack(
        BalanceContract.CrossContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setOwnerChainId(Sha256Hash.wrap(ByteArray
                .fromHexString(OWNER_CHAINID))
                .getByteString())
            .setToChainId(Sha256Hash.wrap(ByteArray
                .fromHexString(TO_CHAINID))
                .getByteString())
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setType(BalanceContract.CrossContract.CrossDataType.TOKEN)
            .setData(getCrossContractData().toByteString())
            .build());
  }


  /**
   * normal cross chain token transaction.
   */
  @Test
  public void crossChainTransferTrxIsSource() {
    try {
      //1.prepare some data
      byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS);
      AccountCapsule ownerAccountCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8(OWNER_ADDRESS),
              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
              AccountType.Normal,
              300_000_000L);
      ownerAccountCapsule.addAssetAmount("1000001".getBytes(), 1000L);

      Map<String, Long> assetMap = new HashMap<>();
      assetMap.put("1000001",1000L);
      ownerAccountCapsule.addAssetMapV2(assetMap);

      AssetIssueCapsule assetIssueCapsule =
          new AssetIssueCapsule(
              AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                  .setName(ByteString.copyFrom("1000001".getBytes()))
                  .build());
      assetIssueCapsule.setId(String.valueOf(1000L));

      dbManager.getAccountStore().put(ownerAddress, ownerAccountCapsule);
      dbManager.getChainBaseManager().getCrossRevokingStore()
              .put(("in_" + OWNER_CHAINID + "_" + "1000001").getBytes(),
          new BytesCapsule(ByteArray.fromLong(1000L)));
      dbManager.getChainBaseManager().getCrossRevokingStore()
              .put(("out_" + TO_CHAINID + "_" + "1000001").getBytes(),
          new BytesCapsule(ByteArray.fromLong(1000L)));
      dbManager.getChainBaseManager().getAssetIssueV2Store()
              .put(assetIssueCapsule.getName().toByteArray(),assetIssueCapsule);
      //2.run test
      CrossChainActuator actuator = new CrossChainActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract());
      trx.setSource(true);
      actuator.setTx(trx);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
  }


}