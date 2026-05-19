package org.tron.core.net.messagehandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.net.message.pbft.PbftCommitMessage;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;

public class PbftDataSyncHandlerTest {
  @Test
  public void testProcessMessage() throws Exception {
    PbftDataSyncHandler pbftDataSyncHandler = new PbftDataSyncHandler();
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY);
    Protocol.PBFTMessage.Raw.Builder rawBuilder = Protocol.PBFTMessage.Raw.newBuilder();
    rawBuilder.setViewN(blockCapsule.getNum())
        .setEpoch(0)
        .setDataType(Protocol.PBFTMessage.DataType.BLOCK)
        .setMsgType(Protocol.PBFTMessage.MsgType.PREPREPARE)
        .setData(blockCapsule.getBlockId().getByteString());
    Protocol.PBFTMessage.Raw raw = rawBuilder.build();
    PbftSignCapsule pbftSignCapsule = new PbftSignCapsule(raw.toByteString(), new ArrayList<>());
    PbftCommitMessage pbftCommitMessage = new PbftCommitMessage(pbftSignCapsule);

    DynamicPropertiesStore dynamicPropertiesStore = Mockito.mock(DynamicPropertiesStore.class);
    PbftSignDataStore pbftSignDataStore = Mockito.mock(PbftSignDataStore.class);
    ChainBaseManager chainBaseManager = Mockito.mock(ChainBaseManager.class);
    Mockito.when(chainBaseManager.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);
    Mockito.when(dynamicPropertiesStore.allowPBFT()).thenReturn(true);
    Mockito.when(dynamicPropertiesStore.getMaintenanceTimeInterval()).thenReturn(600L);
    Mockito.when(chainBaseManager.getPbftSignDataStore()).thenReturn(pbftSignDataStore);

    Field field = PbftDataSyncHandler.class.getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(pbftDataSyncHandler, chainBaseManager);

    pbftDataSyncHandler.processMessage(null, pbftCommitMessage);
    Assert.assertEquals(Protocol.PBFTMessage.Raw.parseFrom(
        pbftCommitMessage.getPBFTCommitResult().getData()).getViewN(), 1);

    pbftDataSyncHandler.processPBFTCommitData(blockCapsule);
    Field field1 = PbftDataSyncHandler.class.getDeclaredField("pbftCommitMessageCache");
    field1.setAccessible(true);
    Map map = new ObjectMapper().convertValue(field1.get(pbftDataSyncHandler), Map.class);
    Assert.assertFalse(map.containsKey(0));
  }

  @Test
  public void testValidPbftSignPaddedSigOsakaRejected() throws Exception {
    PbftDataSyncHandler pbftDataSyncHandler = new PbftDataSyncHandler();
    DynamicPropertiesStore dynamicPropertiesStore = Mockito.mock(DynamicPropertiesStore.class);
    ChainBaseManager chainBaseManager = Mockito.mock(ChainBaseManager.class);
    Mockito.when(chainBaseManager.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);
    Mockito.when(dynamicPropertiesStore.signatureMaxSizeChecked()).thenReturn(true);

    Field field = PbftDataSyncHandler.class.getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(pbftDataSyncHandler, chainBaseManager);

    Param.getInstance().setAgreeNodeCount(1);
    Method method = PbftDataSyncHandler.class.getDeclaredMethod("validPbftSign",
        Protocol.PBFTMessage.Raw.class, java.util.List.class, java.util.List.class);
    method.setAccessible(true);

    Protocol.PBFTMessage.Raw raw = Protocol.PBFTMessage.Raw.newBuilder()
        .setViewN(1)
        .setEpoch(0)
        .setDataType(Protocol.PBFTMessage.DataType.BLOCK)
        .setMsgType(Protocol.PBFTMessage.MsgType.COMMIT)
        .setData(ByteString.copyFromUtf8("block"))
        .build();

    boolean valid = (boolean) method.invoke(pbftDataSyncHandler, raw,
        Collections.singletonList(ByteString.copyFrom(new byte[69])),
        Collections.singletonList(ByteString.copyFrom(new byte[21])));
    Assert.assertFalse(valid);
  }
}
