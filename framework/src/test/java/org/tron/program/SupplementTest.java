package org.tron.program;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigInteger;
import javax.annotation.Resource;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.entity.PeerInfo;
import org.tron.common.utils.CompactEncoder;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.Value;
import org.tron.core.Constant;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.HttpSelfFormatFieldName;
import org.tron.core.store.StorageRowStore;
import org.tron.keystore.WalletUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@ContextConfiguration(classes = {DefaultConfig.class})
public class SupplementTest {

  private static final String dbPath = "output_coverage_test";

  @Resource
  private StorageRowStore storageRowStore;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
  }

  @Test
  public void testGet() throws Exception {
    StorageRowCapsule storageRowCapsule = storageRowStore.get(new byte[]{});
    assertNotNull(storageRowCapsule);

    DbBackupConfig dbBackupConfig = new DbBackupConfig();
    dbBackupConfig.initArgs(true, "propPath", "bak1path/", "bak2path/", 1);

    WalletUtils.generateFullNewWalletFile("123456", new File(dbPath));
    WalletUtils.generateLightNewWalletFile("123456", new File(dbPath));
    WalletUtils.getDefaultKeyDirectory();
    WalletUtils.getTestnetKeyDirectory();
    WalletUtils.getMainnetKeyDirectory();

    Value value = new Value(new byte[]{1});
    value.asBytes();
    value = new Value(1);
    value.asInt();
    value = new Value(100L);
    value.asLong();
    value = new Value(new BigInteger("1000"));
    value.asBigInt();
    value = new Value("1000");
    value.asString();
    value.isEmpty();
    value = new Value(new byte[]{1, 2, 3});
    value.isList();
    value.isReadableString();
    value.isHexString();
    value.isHashCode();
    value.isNull();
    value.length();
    assertNotNull(value.toString());
    value.countBranchNodes();

    PeerInfo peerInfo = new PeerInfo();
    peerInfo.setAvgLatency(peerInfo.getAvgLatency());
    peerInfo.setBlockInPorcSize(peerInfo.getBlockInPorcSize());
    peerInfo.setConnectTime(peerInfo.getConnectTime());
    peerInfo.setDisconnectTimes(peerInfo.getDisconnectTimes());
    peerInfo.setHeadBlockTimeWeBothHave(peerInfo.getHeadBlockTimeWeBothHave());
    peerInfo.setHeadBlockWeBothHave(peerInfo.getHeadBlockWeBothHave());
    peerInfo.setHost(peerInfo.getHost());
    peerInfo.setInFlow(peerInfo.getInFlow());
    peerInfo.setLastBlockUpdateTime(peerInfo.getLastBlockUpdateTime());
    peerInfo.setLastSyncBlock(peerInfo.getLastSyncBlock());
    peerInfo.setLocalDisconnectReason(peerInfo.getLocalDisconnectReason());
    peerInfo.setNodeCount(peerInfo.getNodeCount());
    peerInfo.setNodeId(peerInfo.getNodeId());
    peerInfo.setRemainNum(peerInfo.getRemainNum());
    peerInfo.setRemoteDisconnectReason(peerInfo.getRemoteDisconnectReason());
    peerInfo.setScore(peerInfo.getScore());
    peerInfo.setPort(peerInfo.getPort());
    peerInfo.setSyncFlag(peerInfo.isSyncFlag());
    peerInfo.setNeedSyncFromPeer(peerInfo.isNeedSyncFromPeer());
    peerInfo.setNeedSyncFromUs(peerInfo.isNeedSyncFromUs());
    peerInfo.setSyncToFetchSize(peerInfo.getSyncToFetchSize());
    peerInfo.setSyncToFetchSizePeekNum(peerInfo.getSyncToFetchSizePeekNum());
    peerInfo.setSyncBlockRequestedSize(peerInfo.getSyncBlockRequestedSize());
    peerInfo.setUnFetchSynNum(peerInfo.getUnFetchSynNum());
    peerInfo.setActive(peerInfo.isActive());

    assertNotNull(JsonUtil.json2Obj("{}", PeerInfo.class));
    assertNotNull(JsonUtil.obj2Json(peerInfo));

    assertTrue(HttpSelfFormatFieldName.isAddressFormat(
        "protocol.DelegatedResourceMessage.fromAddress"));
    assertTrue(HttpSelfFormatFieldName.isNameStringFormat(
        "protocol.MarketPriceList.buy_token_id"));

    CompactEncoder.packNibbles(new byte[] {1,2,3,4,5,6,7});
    assertFalse(CompactEncoder.hasTerminator(new byte[] {1,2,3,4,5,6,7}));
    CompactEncoder.unpackToNibbles(new byte[] {1,2,3,4,5,6,7});
    CompactEncoder.binToNibblesNoTerminator(new byte[] {1,2,3,4,5,6,7});

    assertNotNull(RLP.decodeIP4Bytes(new byte[] {1,2,3,4,5,6,7}, 0));
    RLP.decodeByteArray(new byte[] {1,2,3,4,5,6,7}, 0);
    RLP.nextItemLength(new byte[] {1,2,3,4,5,6,7}, 0);
    RLP.decodeStringItem(new byte[] {1,2,3,4,5,6,7}, 0);
    RLP.decodeInt(new byte[] {1,2,3,4,5,6,7}, 0);
    RLP.decode2OneItem(new byte[] {1,2,3,4,5,6,7}, 0);
    RLP.decode2(new byte[] {1,2,3,4,5,6,7}, 1);
    RLP.decode2(new byte[] {1,2,3,4,5,6,7});
    thrown.expect(ClassCastException.class);
    RLP.unwrapList(new byte[] {1,2,3,4,5,6,7});
  }

}
