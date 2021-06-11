package org.tron.core.ibc.spv;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol;


@Slf4j
public class HeaderManagerTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private ChainBaseManager chainBaseManager;

  private HeaderManager headerManager;

  private static String KEY_11 = "7ac4ea5ea21f5fdc23eb6f192fa0da4677f0d94cc5b716feac429b97d4336871";
  private static String KEY_12 = "98fc1e0fcdd9699ff65f81014c0e108bb53729ac46c34b9b115fa79c82023c64";


  private static String addressHexString_KEY_11 = "a07d9333c36c1392cd3350927820aa5a88314cd3d2";

  private static List srList = new ArrayList();
  private static String CHAIN_ID =
      "00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d11";

  /**
   * init static var
   */
  static {
    Args.setParam(new String[]{"--output-directory", "dbPath-HeaderManagerTest"},
        Constant.TEST_CONF);
  }

  @BeforeClass
  public static void init() {
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
  }

  @Before
  public void initParas() {
    chainBaseManager = dbManager.getChainBaseManager();
    headerManager = context.getBean(HeaderManager.class);
  }


  /**
   * release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("dbPath-HeaderManagerTest"));
  }

  private ECKey.ECDSASignature buildSign(byte[] hash, String priKey)  {
    ECKey ecKey = ECKey.fromPrivate(ByteArray.fromHexString(priKey));
    ECKey.ECDSASignature signature = ecKey.sign(hash);
    return signature;
  }

  private byte[] getBlockPbftData(Protocol.BlockHeader header, Long maintenanceTime) {
    Protocol.PBFTMessage.Raw.Builder rawBuilder = Protocol.PBFTMessage.Raw.newBuilder();
    rawBuilder.setViewN(header.getRawData().getNumber()).setEpoch(maintenanceTime)
        .setDataType(Protocol.PBFTMessage.DataType.BLOCK)
        .setMsgType(Protocol.PBFTMessage.MsgType.COMMIT)
        .setData(new BlockHeaderCapsule(header).getBlockId().getByteString());
    Protocol.PBFTMessage.Raw raw = rawBuilder.build();
    return Sha256Hash.hash(true, raw.toByteArray());
  }

  private BlockCapsule buildBlockCapsule() {
    BlockCapsule blockCapsule =
        new BlockCapsule(100,
            Sha256Hash.wrap(chainBaseManager.getGenesisBlockId().getByteString()), 99,
            ByteString.copyFromUtf8(addressHexString_KEY_11),
            Sha256Hash.wrap(ByteArray.fromHexString(CHAIN_ID)).getByteString());
    return blockCapsule;
  }

  private Protocol.SignedBlockHeader buildSignedBlockHeader() throws SignatureException {
    BlockCapsule blockCapsule = buildBlockCapsule();
    BlockHeaderCapsule blockHeaderCapsule = new BlockHeaderCapsule(
        blockCapsule.getInstance().getBlockHeader());

    byte[] hash = getBlockPbftData(blockHeaderCapsule.getInstance(), 300000L);
    ECKey.ECDSASignature sign1 = buildSign(hash, KEY_11);
    ECKey.ECDSASignature sign2 = buildSign(hash, KEY_12);
    srList.add(ByteString.copyFrom(ECKey.signatureToAddress(hash,sign1)));
    srList.add(ByteString.copyFrom(ECKey.signatureToAddress(hash,sign2)));
    Protocol.SRL.Builder srlBuilder = Protocol.SRL.newBuilder();
    srlBuilder.addAllSrAddress(srList);
    Protocol.PBFTMessage.Raw pbftMsgRaw = Protocol.PBFTMessage.Raw.newBuilder()
        .setData(srlBuilder.build().toByteString())
        .setEpoch(300000L).build();
    Protocol.PBFTCommitResult.Builder builder = Protocol.PBFTCommitResult.newBuilder();
    builder.setData(pbftMsgRaw.toByteString());

    builder.addSignature(ByteString.copyFrom(sign1.toByteArray()));
    builder.addSignature(ByteString.copyFrom(sign2.toByteArray()));
    Protocol.PBFTCommitResult pbftCommitResult = builder.build();

    Protocol.SignedBlockHeader.Builder signedBlockHeaderBuilder = Protocol
        .SignedBlockHeader.newBuilder();
    signedBlockHeaderBuilder.setBlockHeader(blockHeaderCapsule.getInstance());
    signedBlockHeaderBuilder.addAllSrsSignature(pbftCommitResult.getSignatureList());
    signedBlockHeaderBuilder.setSrList(pbftCommitResult);
    return signedBlockHeaderBuilder.build();
  }

  @Test
  public void pushBlockHeader() throws ValidateSignatureException,
      InvalidProtocolBufferException, BadBlockException, SignatureException {
    Protocol.SignedBlockHeader signedBlockHeader = buildSignedBlockHeader();
    chainBaseManager.getCommonDataBase().saveSRL(CHAIN_ID, 300000L,
        signedBlockHeader.getSrList());
    try {
      //check agreeNode not match
      chainBaseManager.getCommonDataBase().saveAgreeNodeCount(CHAIN_ID, 27);
      headerManager.pushBlockHeader(signedBlockHeader);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ValidateSignatureException);
    }

    //check sr sign validate succeed and SrList validate succeed
    chainBaseManager.getCommonDataBase().saveChainMaintenanceTimeInterval(CHAIN_ID,
        300000L);
    chainBaseManager.getCommonDataBase().saveAgreeNodeCount(CHAIN_ID, 1);

    chainBaseManager.getCommonDataBase().saveLatestBlockHeaderHash(CHAIN_ID,
        buildBlockCapsule().getParentBlockId().toString());
    chainBaseManager.getCommonDataBase().saveLatestHeaderBlockNum(CHAIN_ID,
        99,false);
    headerManager.pushBlockHeader(signedBlockHeader);

    Assert.assertNotNull(chainBaseManager.getPbftSignDataStore()
        .getCrossBlockSignData(CHAIN_ID, 100));
    Assert.assertEquals(chainBaseManager.getCommonDataBase().getLatestBlockHeaderHash(CHAIN_ID),
        "00000000000000640672b8e38ebd5f17d2dcc36db38ba2a02ae0beb45c4db728");
    Assert.assertEquals(chainBaseManager.getCommonDataBase().getLatestHeaderBlockNum(CHAIN_ID),
        100);
    Assert.assertEquals(chainBaseManager.getCommonDataBase().getLatestPBFTBlockNum(CHAIN_ID),
        100);
  }

}
