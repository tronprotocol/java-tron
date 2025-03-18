package org.tron.common;

import com.google.protobuf.ByteString;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.tron.common.application.Application;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.store.AccountStore;
import org.tron.protos.Protocol;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultConfig.class})
@DirtiesContext
public abstract class BaseTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Resource
  protected Manager dbManager;
  @Resource
  protected ChainBaseManager chainBaseManager;

  @Resource
  protected Application appT;

  private static Application appT1;


  @PostConstruct
  private void prepare() {
    appT1 = appT;
  }

  public static String dbPath() {
    try {
      return temporaryFolder.newFolder().toString();
    } catch (IOException e) {
      Assert.fail("create temp folder failed");
    }
    return null;
  }

  @AfterClass
  public static void destroy() {
    appT1.shutdown();
    Args.clearParam();
  }

  public Protocol.Block getSignedBlock(ByteString witness, long time, byte[] privateKey) {
    long blockTime = System.currentTimeMillis() / 3000 * 3000;
    if (time != 0) {
      blockTime = time;
    } else {
      if (chainBaseManager.getHeadBlockId().getNum() != 0) {
        blockTime = chainBaseManager.getHeadBlockTimeStamp() + 3000;
      }
    }
    Param param = Param.getInstance();
    Param.Miner miner = param.new Miner(privateKey, witness, witness);
    BlockCapsule blockCapsule = dbManager
        .generateBlock(miner, time, System.currentTimeMillis() + 1000);
    Protocol.Block block = blockCapsule.getInstance();

    Protocol.BlockHeader.raw raw = block.getBlockHeader().getRawData().toBuilder()
        .setParentHash(ByteString
            .copyFrom(chainBaseManager.getDynamicPropertiesStore()
                .getLatestBlockHeaderHash().getBytes()))
        .setNumber(chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1)
        .setTimestamp(blockTime)
        .setWitnessAddress(witness)
        .build();

    ECKey ecKey = ECKey.fromPrivate(privateKey);
    assert ecKey != null;
    ECKey.ECDSASignature signature = ecKey.sign(Sha256Hash.of(CommonParameter
        .getInstance().isECKeyCryptoEngine(), raw.toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toByteArray());

    Protocol.BlockHeader blockHeader = block.getBlockHeader().toBuilder()
        .setRawData(raw)
        .setWitnessSignature(sign)
        .build();

    return block.toBuilder().setBlockHeader(blockHeader).build();
  }

  public void adjustBalance(AccountStore accountStore, byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    Commons.adjustBalance(accountStore, accountAddress, amount,
        chainBaseManager.getDynamicPropertiesStore().disableJavaLangMath());
  }
}
