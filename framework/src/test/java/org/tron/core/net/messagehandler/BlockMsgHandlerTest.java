package org.tron.core.net.messagehandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.P2pException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.relay.RelayService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class BlockMsgHandlerTest extends BaseTest {

  @Resource
  private BlockMsgHandler handler;
  @Resource
  private PeerConnection peer;

  /**
   * init context.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);
  }

  @Before
  public void before() throws Exception {
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.1", 100);
    Field field = c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());
    peer.setChannel(c1);
  }

  @Test
  public void testProcessMessage() {
    BlockCapsule blockCapsule;
    BlockMessage msg;
    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      assertEquals("no request", e.getMessage());
    }

    try {
      List<Transaction> transactionList = ImmutableList.of(
          Transaction.newBuilder()
              .setRawData(Transaction.raw.newBuilder()
                  .setData(
                      ByteString.copyFrom(
                          new byte[Parameter.ChainConstant.BLOCK_SIZE + Constant.ONE_THOUSAND])))
              .build());
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH.getByteString(),
          System.currentTimeMillis() + 10000, transactionList);
      msg = new BlockMessage(blockCapsule);
      System.out.println("len = " + blockCapsule.getInstance().getSerializedSize());
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      //System.out.println(e);
      assertEquals("block size over limit", e.getMessage());
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 10000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      //System.out.println(e);
      assertEquals("block time error", e.getMessage());
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 1000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getSyncBlockRequested()
          .put(msg.getBlockId(), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      //System.out.println(e);
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 1000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (NullPointerException | P2pException e) {
      logger.error("error", e);
    }
  }

  @Test
  public void testProcessBlock() {
    TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);

    try {
      Field field = handler.getClass().getDeclaredField("tronNetDelegate");
      field.setAccessible(true);
      field.set(handler, tronNetDelegate);

      BlockCapsule blockCapsule0 = new BlockCapsule(1,
          Sha256Hash.wrap(ByteString
              .copyFrom(ByteArray
                  .fromHexString(
                      "9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
          1234,
          ByteString.copyFrom("1234567".getBytes()));

      peer.getAdvInvReceive()
          .put(new Item(blockCapsule0.getBlockId(), InventoryType.BLOCK),
              System.currentTimeMillis());

      Mockito.doReturn(true).when(tronNetDelegate).validAdvBlock(peer, blockCapsule0);
      Mockito.doReturn(true).when(tronNetDelegate).containBlock(any(BlockId.class));
      Mockito.doReturn(blockCapsule0.getBlockId()).when(tronNetDelegate).getHeadBlockId();
      Mockito.doNothing().when(tronNetDelegate).processBlock(any(BlockCapsule.class), anyBoolean());
      List<PeerConnection> peers = new ArrayList<>();
      peers.add(peer);
      Mockito.doReturn(peers).when(tronNetDelegate).getActivePeer();

      Method method = handler.getClass()
          .getDeclaredMethod("processBlock", PeerConnection.class, BlockCapsule.class);
      method.setAccessible(true);
      method.invoke(handler, peer, blockCapsule0);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testPaddedWitnessIngress() throws Exception {
    BlockCapsule canonical = signedWitnessBlock();
    ByteString signature = canonical.getInstance().getBlockHeader().getWitnessSignature();
    for (boolean strict : new boolean[]{false, true}) {
      TronNetDelegate delegate = witnessDelegate(strict, canonical.getWitnessAddress());
      AdvService adv = Mockito.mock(AdvService.class);
      BlockMsgHandler ingress = witnessHandler(delegate, adv, Mockito.mock(SyncService.class));
      BlockMessage message = withWitnessSignature(canonical,
          signature.concat(ByteString.copyFrom(new byte[3])));
      PeerConnection sender = witnessPeer();
      sender.getAdvInvRequest().put(new Item(message.getBlockId(), InventoryType.BLOCK), 0L);

      ingress.processMessage(sender, message);

      ArgumentCaptor<BlockCapsule> processed = ArgumentCaptor.forClass(BlockCapsule.class);
      Mockito.verify(delegate).processBlock(processed.capture(), Mockito.eq(false));
      Assert.assertEquals(canonical.getInstance(), processed.getValue().getInstance());
      ArgumentCaptor<BlockMessage> forwarded = ArgumentCaptor.forClass(BlockMessage.class);
      Mockito.verify(adv).broadcast(forwarded.capture());
      Assert.assertArrayEquals(canonical.getData(), forwarded.getValue().getData());
      Assert.assertArrayEquals(canonical.getData(), message.getData());
      Assert.assertEquals(canonical.getBlockId(), message.getBlockId());
      Assert.assertTrue(sender.getAdvInvRequest().isEmpty());
    }
  }

  @Test
  public void testInvalidWitnessIngress() throws Exception {
    BlockCapsule canonical = signedWitnessBlock();
    ByteString signature = canonical.getInstance().getBlockHeader().getWitnessSignature();
    for (boolean strict : new boolean[]{false, true}) {
      for (ByteString invalid : Arrays.asList(signature.substring(0, 64),
          ByteString.copyFrom(new byte[68]))) {
        TronNetDelegate delegate = witnessDelegate(strict, canonical.getWitnessAddress());
        AdvService adv = Mockito.mock(AdvService.class);
        BlockMsgHandler ingress = witnessHandler(delegate, adv, Mockito.mock(SyncService.class));
        BlockMessage message = withWitnessSignature(canonical, invalid);
        PeerConnection sender = witnessPeer();
        sender.getAdvInvRequest().put(new Item(message.getBlockId(), InventoryType.BLOCK), 0L);

        P2pException error = Assert.assertThrows(P2pException.class,
            () -> ingress.processMessage(sender, message));

        Assert.assertEquals(P2pException.TypeEnum.BLOCK_SIGN_INVALID, error.getType());
        Mockito.verify(delegate, Mockito.never()).processBlock(any(BlockCapsule.class),
            anyBoolean());
        Mockito.verify(adv, Mockito.never()).broadcast(any());
      }
    }
  }

  @Test
  public void testFutureBlockRejectedBeforeAdvertisedOrSyncProcessing() throws Exception {
    for (boolean syncResponse : new boolean[]{false, true}) {
      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 60000, ByteString.EMPTY);
      BlockMessage message = new BlockMessage(block);
      PeerConnection sender = witnessPeer();
      if (syncResponse) {
        sender.getSyncBlockRequested().put(message.getBlockId(), 0L);
      } else {
        sender.getAdvInvRequest().put(new Item(message.getBlockId(), InventoryType.BLOCK), 0L);
      }
      TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
      AdvService adv = Mockito.mock(AdvService.class);
      SyncService sync = Mockito.mock(SyncService.class);
      BlockMsgHandler ingress = witnessHandler(delegate, adv, sync);

      P2pException error = Assert.assertThrows(P2pException.class,
          () -> ingress.processMessage(sender, message));

      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, error.getType());
      Assert.assertEquals("block time error", error.getMessage());
      Mockito.verifyNoInteractions(delegate, adv, sync);
    }
  }

  @Test
  public void testInvalidContractCountRejectedBeforeBroadcast() throws Exception {
    for (boolean fastForward : new boolean[]{false, true}) {
      for (int count : new int[]{0, 2}) {
        ECKey key = ECKey.fromPrivate(BigInteger.TEN);
        BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 1234,
            ByteString.copyFrom(key.getAddress()));
        block.addTransaction(new TransactionCapsule(Transaction.newBuilder()
            .setRawData(Transaction.raw.newBuilder()
                .setData(ByteString.copyFrom(new byte[80])).addContract(
                Transaction.Contract.newBuilder()
                    .setType(Transaction.Contract.ContractType.TransferContract))).build()));
        Transaction.raw.Builder raw = Transaction.raw.newBuilder().setTimestamp(1235);
        for (int i = 0; i < count; i++) {
          raw.addContract(Transaction.Contract.newBuilder()
              .setType(Transaction.Contract.ContractType.TransferContract));
        }
        block.addTransaction(new TransactionCapsule(
            Transaction.newBuilder().setRawData(raw).build()));
        block.setMerkleRoot();
        block.sign(key.getPrivKeyBytes());
        TronNetDelegate delegate = witnessDelegate(false, block.getWitnessAddress());
        Manager manager = (Manager) ReflectUtils.getFieldObject(delegate, "dbManager");
        Assert.assertTrue(block.validateSignature(manager.getDynamicPropertiesStore(),
            manager.getAccountStore()));
        AdvService adv = Mockito.mock(AdvService.class);
        RelayService relay = Mockito.mock(RelayService.class);
        BlockMsgHandler ingress = witnessHandler(delegate, adv, Mockito.mock(SyncService.class));
        ReflectUtils.setFieldValue(ingress, "fastForward", fastForward);
        ReflectUtils.setFieldValue(ingress, "relayService", relay);
        BlockMessage message = new BlockMessage(block);
        PeerConnection sender = witnessPeer();
        sender.getAdvInvRequest().put(new Item(message.getBlockId(), InventoryType.BLOCK), 0L);

        P2pException error = Assert.assertThrows(P2pException.class,
            () -> ingress.processMessage(sender, message));

        Assert.assertEquals(P2pException.TypeEnum.BAD_BLOCK, error.getType());
        Assert.assertTrue(error.getMessage().contains("count: " + count));
        Mockito.verify(adv, Mockito.never()).broadcast(any());
        Mockito.verify(relay, Mockito.never()).broadcast(any());
        Mockito.verify(delegate, Mockito.never()).processBlock(any(BlockCapsule.class),
            anyBoolean());
      }
    }
  }

  @Test
  public void testUndersizedLaterTransactionRejectedBeforeBroadcast() throws Exception {
    for (boolean fastForward : new boolean[]{false, true}) {
      for (int size : new int[]{64, 79}) {
        BlockCapsule block = signedWitnessBlock();
        Transaction transaction = Transaction.newBuilder()
            .setRawData(Transaction.raw.newBuilder()
                .setData(ByteString.copyFrom(new byte[size - 8])).addContract(
                    Transaction.Contract.newBuilder()
                        .setType(Transaction.Contract.ContractType.TransferContract))).build();
        Assert.assertEquals(size, transaction.getSerializedSize());
        block.addTransaction(new TransactionCapsule(transaction));
        block.setMerkleRoot();
        block.sign(ECKey.fromPrivate(BigInteger.TEN).getPrivKeyBytes());
        TronNetDelegate delegate = witnessDelegate(false, block.getWitnessAddress());
        AdvService adv = Mockito.mock(AdvService.class);
        RelayService relay = Mockito.mock(RelayService.class);
        BlockMsgHandler ingress = witnessHandler(delegate, adv, Mockito.mock(SyncService.class));
        ReflectUtils.setFieldValue(ingress, "fastForward", fastForward);
        ReflectUtils.setFieldValue(ingress, "relayService", relay);
        BlockMessage message = new BlockMessage(block);
        PeerConnection sender = witnessPeer();
        sender.getAdvInvRequest().put(new Item(message.getBlockId(), InventoryType.BLOCK), 0L);

        P2pException error = Assert.assertThrows(P2pException.class,
            () -> ingress.processMessage(sender, message));

        Assert.assertEquals(P2pException.TypeEnum.BAD_BLOCK, error.getType());
        Assert.assertTrue(error.getMessage().contains("size: " + size));
        Mockito.verify(adv, Mockito.never()).broadcast(any());
        Mockito.verify(relay, Mockito.never()).broadcast(any());
        Mockito.verify(delegate, Mockito.never()).processBlock(any(BlockCapsule.class),
            anyBoolean());
      }
    }
  }

  @Test
  public void testInvalidMerkleRootRejectedBeforeBroadcast() throws Exception {
    for (boolean fastForward : new boolean[]{false, true}) {
      BlockCapsule block = signedWitnessBlock();
      block.addTransaction(new TransactionCapsule(Transaction.newBuilder()
          .setRawData(Transaction.raw.newBuilder().setTimestamp(1235)
              .setData(ByteString.copyFrom(new byte[80])).addContract(
              Transaction.Contract.newBuilder()
                  .setType(Transaction.Contract.ContractType.TransferContract))).build()));
      TronNetDelegate delegate = witnessDelegate(false, block.getWitnessAddress());
      Manager manager = (Manager) ReflectUtils.getFieldObject(delegate, "dbManager");
      Assert.assertTrue(block.validateSignature(manager.getDynamicPropertiesStore(),
          manager.getAccountStore()));
      AdvService adv = Mockito.mock(AdvService.class);
      RelayService relay = Mockito.mock(RelayService.class);
      BlockMsgHandler ingress = witnessHandler(delegate, adv, Mockito.mock(SyncService.class));
      ReflectUtils.setFieldValue(ingress, "fastForward", fastForward);
      ReflectUtils.setFieldValue(ingress, "relayService", relay);
      BlockMessage message = new BlockMessage(block);
      PeerConnection sender = witnessPeer();
      sender.getAdvInvRequest().put(new Item(message.getBlockId(), InventoryType.BLOCK), 0L);

      P2pException error = Assert.assertThrows(P2pException.class,
          () -> ingress.processMessage(sender, message));

      Assert.assertEquals(P2pException.TypeEnum.BLOCK_MERKLE_INVALID, error.getType());
      Mockito.verify(adv, Mockito.never()).broadcast(any());
      Mockito.verify(relay, Mockito.never()).broadcast(any());
      Mockito.verify(delegate, Mockito.never()).processBlock(any(BlockCapsule.class),
          anyBoolean());
    }
  }

  @Test
  public void testPaddedWitnessSyncHandoff() throws Exception {
    BlockCapsule canonical = signedWitnessBlock();
    ByteString signature = canonical.getInstance().getBlockHeader().getWitnessSignature();
    BlockMessage message = withWitnessSignature(canonical,
        signature.concat(ByteString.copyFrom(new byte[3])));
    SyncService sync = Mockito.mock(SyncService.class);
    BlockMsgHandler ingress = witnessHandler(Mockito.mock(TronNetDelegate.class),
        Mockito.mock(AdvService.class), sync);
    PeerConnection sender = witnessPeer();
    sender.getSyncBlockRequested().put(message.getBlockId(), 0L);

    ingress.processMessage(sender, message);

    ArgumentCaptor<BlockMessage> queued = ArgumentCaptor.forClass(BlockMessage.class);
    Mockito.verify(sync).processBlock(Mockito.eq(sender), queued.capture());
    Assert.assertArrayEquals(canonical.getData(), queued.getValue().getData());
    Assert.assertEquals(canonical.getInstance(), queued.getValue().getBlockCapsule().getInstance());
    Assert.assertTrue(sender.getSyncBlockRequested().isEmpty());
    Assert.assertTrue(sender.getSyncBlockInProcess().contains(canonical.getBlockId()));
  }

  private BlockCapsule signedWitnessBlock() {
    ECKey key = ECKey.fromPrivate(BigInteger.TEN);
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 1234,
        ByteString.copyFrom(key.getAddress()));
    // A padded transaction signature must survive witness normalization, including sync handoff.
    Transaction transaction = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().setTimestamp(1234)
            .setData(ByteString.copyFrom(new byte[80]))
            .addContract(Transaction.Contract.newBuilder()
                .setType(Transaction.Contract.ContractType.TransferContract)))
        .addSignature(ByteString.copyFrom(new byte[68])).build();
    block.addTransaction(new TransactionCapsule(transaction));
    block.setMerkleRoot();
    block.sign(key.getPrivKeyBytes());
    return block;
  }

  private BlockMessage withWitnessSignature(BlockCapsule block, ByteString signature)
      throws Exception {
    return new BlockMessage(block.getInstance().toBuilder()
        .setBlockHeader(block.getInstance().getBlockHeader().toBuilder()
            .setWitnessSignature(signature)).build().toByteArray());
  }

  private TronNetDelegate witnessDelegate(boolean strict, ByteString witness) throws Exception {
    DynamicPropertiesStore properties = Mockito.mock(DynamicPropertiesStore.class);
    Mockito.when(properties.allowStrictEcdsaValidation()).thenReturn(strict);
    Manager manager = Mockito.mock(Manager.class);
    Mockito.when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    Mockito.when(manager.getAccountStore()).thenReturn(Mockito.mock(AccountStore.class));
    WitnessScheduleStore schedule = Mockito.mock(WitnessScheduleStore.class);
    Mockito.when(schedule.getActiveWitnesses()).thenReturn(Collections.singletonList(witness));
    TronNetDelegate delegate = Mockito.spy(new TronNetDelegate());
    ReflectUtils.setFieldValue(delegate, "dbManager", manager);
    ReflectUtils.setFieldValue(delegate, "witnessScheduleStore", schedule);
    Mockito.doReturn(true).when(delegate).containBlock(any(BlockId.class));
    Mockito.doReturn(new BlockId(Sha256Hash.ZERO_HASH, 0)).when(delegate).getHeadBlockId();
    Mockito.doReturn(Collections.emptyList()).when(delegate).getActivePeer();
    Mockito.doNothing().when(delegate).processBlock(any(BlockCapsule.class), anyBoolean());
    return delegate;
  }

  private BlockMsgHandler witnessHandler(TronNetDelegate delegate, AdvService adv, SyncService sync)
      throws Exception {
    BlockMsgHandler ingress = new BlockMsgHandler();
    ReflectUtils.setFieldValue(ingress, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(ingress, "advService", adv);
    ReflectUtils.setFieldValue(ingress, "syncService", sync);
    ReflectUtils.setFieldValue(ingress, "fetchBlockService", Mockito.mock(FetchBlockService.class));
    ReflectUtils.setFieldValue(ingress, "witnessProductBlockService",
        Mockito.mock(WitnessProductBlockService.class));
    ReflectUtils.setFieldValue(ingress, "fastForward", false);
    return ingress;
  }

  private PeerConnection witnessPeer() {
    InetSocketAddress address = new InetSocketAddress("127.0.0.254", 10001);
    Channel channel = Mockito.mock(Channel.class);
    Mockito.when(channel.getInetAddress()).thenReturn(address.getAddress());
    Mockito.when(channel.getInetSocketAddress()).thenReturn(address);
    PeerConnection sender = new PeerConnection();
    sender.setChannel(channel);
    return sender;
  }
}
