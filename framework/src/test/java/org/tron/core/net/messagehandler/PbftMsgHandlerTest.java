package org.tron.core.net.messagehandler;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.PbftBaseImpl;
import org.tron.core.exception.P2pException;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;


public class PbftMsgHandlerTest {
  private static TronApplicationContext context;
  private PeerConnection peer;
  private static String dbPath = "output-pbft-message-handler-test";


  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);

    TronNetService tronNetService = context.getBean(TronNetService.class);
    Parameter.p2pConfig = new P2pConfig();
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", Parameter.p2pConfig);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Before
  public void clearPeers() {
    try {
      Field field = PeerManager.class.getDeclaredField("peers");
      field.setAccessible(true);
      field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      //ignore
    }
  }

  @Test
  public void testPbft() throws Exception {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(context, c1);
    Assert.assertEquals(1, PeerManager.getPeers().size());
    Assert.assertFalse(c1.isDisconnect());

    peer = PeerManager.getPeers().get(0);
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY);
    PbftMessage pbftMessage = new PbftMessage();
    Protocol.PBFTMessage.Raw.Builder rawBuilder = Protocol.PBFTMessage.Raw.newBuilder();
    Protocol.PBFTMessage.Builder builder = Protocol.PBFTMessage.newBuilder();
    rawBuilder.setViewN(blockCapsule.getNum())
        .setEpoch(0)
        .setDataType(Protocol.PBFTMessage.DataType.BLOCK)
        .setMsgType(Protocol.PBFTMessage.MsgType.PREPREPARE)
        .setData(blockCapsule.getBlockId().getByteString());
    Protocol.PBFTMessage.Raw raw = rawBuilder.build();
    builder.setRawData(raw);
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(PublicMethod.getRandomPrivateKey()),
        true);
    builder.setSignature(ByteString.copyFrom(sign.Base64toBytes(sign.signHash(
        Sha256Hash.hash(true, raw.toByteArray())))));
    Protocol.PBFTMessage message = builder.build();
    pbftMessage.setType(MessageTypes.PBFT_MSG.asByte());
    pbftMessage.setPbftMessage(message);
    pbftMessage.setData(message.toByteArray());
    pbftMessage.setSwitch(blockCapsule.isSwitch());
    Param.getInstance().setPbftInterface(context.getBean(PbftBaseImpl.class));
    peer.setNeedSyncFromPeer(false);
    //Mockito.doNothing().when(pbftMessage).analyzeSignature();
    try {
      context.getBean(PbftMsgHandler.class).processMessage(peer, pbftMessage);
    } catch (P2pException e) {
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, e.getType());
    }

    DynamicPropertiesStore dynamicPropertiesStore = context.getBean(DynamicPropertiesStore.class);
    dynamicPropertiesStore.saveAllowPBFT(1);
    try {
      context.getBean(PbftMsgHandler.class).processMessage(peer, pbftMessage);
    } catch (P2pException e) {
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, e.getType());
    }

    Assert.assertEquals(1, PeerManager.getPeers().size());
  }
}
