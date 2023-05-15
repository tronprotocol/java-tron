package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

@Slf4j
public class TransactionExpireTest {

  private String dbPath = "output_expire_test";
  private TronApplicationContext context;
  private Wallet wallet;
  private Manager dbManager;
  private BlockCapsule blockCapsule;

  @Before
  public void init() {
    Args.setParam(new String[] {"--output-directory", dbPath}, Constant.TEST_CONF);
    CommonParameter.PARAMETER.setMinEffectiveConnection(0);

    context = new TronApplicationContext(DefaultConfig.class);
    wallet = context.getBean(Wallet.class);
    dbManager = context.getBean(Manager.class);

    blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(blockCapsule.getNum());
    dbManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
    dbManager.updateRecentBlock(blockCapsule);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testExpireTransaction() {
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom(Args.getLocalWitnesses()
            .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine())))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();
    TransactionCapsule transactionCapsule =
        new TransactionCapsule(transferContract, ContractType.TransferContract);
    transactionCapsule.setReference(blockCapsule.getNum(), blockCapsule.getBlockId().getBytes());
    Assert.assertEquals(1, blockCapsule.getTimeStamp());

    long blockTimeStamp = blockCapsule.getTimeStamp();
    transactionCapsule.setExpiration(blockTimeStamp - 1);
    transactionCapsule.sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));

    GrpcAPI.Return result = wallet.broadcastTransaction(transactionCapsule.getInstance());
    Assert.assertEquals(response_code.TRANSACTION_EXPIRATION_ERROR, result.getCode());
  }
}
