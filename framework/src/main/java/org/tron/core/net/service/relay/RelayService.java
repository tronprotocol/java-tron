package org.tron.core.net.service.relay;

import com.google.protobuf.ByteString;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.backup.BackupManager.BackupStatusEnum;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Component
public class RelayService {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private ApplicationContext ctx;

  private Manager manager;

  private WitnessScheduleStore witnessScheduleStore;

  private BackupManager backupManager;

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private CommonParameter parameter = Args.getInstance();

  private List<InetSocketAddress> fastForwardNodes = parameter.getFastForwardNodes();

  private ByteString witnessAddress = ByteString
      .copyFrom(Args.getLocalWitnesses().getWitnessAccountAddress(CommonParameter.getInstance()
          .isECKeyCryptoEngine()));

  private int keySize = Args.getLocalWitnesses().getPrivateKeys().size();

  private int maxFastForwardNum = Args.getInstance().getMaxFastForwardNum();

  public void init() {
    manager = ctx.getBean(Manager.class);
    witnessScheduleStore = ctx.getBean(WitnessScheduleStore.class);
    backupManager = ctx.getBean(BackupManager.class);

    logger.info("Fast forward config, isWitness: {}, keySize: {}, fastForwardNodes: {}",
        parameter.isWitness(), keySize, fastForwardNodes.size());

    if (!parameter.isWitness() || keySize == 0 || fastForwardNodes.isEmpty()) {
      return;
    }

    executorService.scheduleWithFixedDelay(() -> {
      try {
        if (witnessScheduleStore.getActiveWitnesses().contains(witnessAddress)
            && backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
          connect();
        } else {
          disconnect();
        }
      } catch (Exception e) {
        logger.info("Execute failed.", e);
      }
    }, 30, 100, TimeUnit.SECONDS);
  }

  public void close() {
    executorService.shutdown();
  }

  public void fillHelloMessage(HelloMessage message, Channel channel) {
    if (isActiveWitness()) {
      fastForwardNodes.forEach(address -> {
        if (address.getAddress().equals(channel.getInetAddress())) {
          SignInterface cryptoEngine = SignUtils
              .fromPrivate(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()),
                  Args.getInstance().isECKeyCryptoEngine());

          ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
              .signHash(Sha256Hash.of(CommonParameter.getInstance()
                  .isECKeyCryptoEngine(), ByteArray.fromLong(message
                  .getTimestamp())).getBytes())));
          message.setHelloMessage(message.getHelloMessage().toBuilder()
              .setAddress(witnessAddress).setSignature(sig).build());
        }
      });
    }
  }

  public boolean checkHelloMessage(HelloMessage message, Channel channel) {
    if (!parameter.isFastForward()) {
      return true;
    }

    Protocol.HelloMessage msg = message.getHelloMessage();

    if (msg.getAddress() == null || msg.getAddress().isEmpty()) {
      logger.info("HelloMessage from {}, address is empty.", channel.getInetAddress());
      return false;
    }

    if (!witnessScheduleStore.getActiveWitnesses().contains(msg.getAddress())) {
      logger.warn("HelloMessage from {}, {} is not a schedule witness.",
          channel.getInetAddress(),
          ByteArray.toHexString(msg.getAddress().toByteArray()));
      return false;
    }

    boolean flag;
    try {
      Sha256Hash hash = Sha256Hash.of(CommonParameter
          .getInstance().isECKeyCryptoEngine(), ByteArray.fromLong(msg.getTimestamp()));
      String sig =
          TransactionCapsule.getBase64FromByteString(msg.getSignature());
      byte[] sigAddress = SignUtils.signatureToAddress(hash.getBytes(), sig,
          Args.getInstance().isECKeyCryptoEngine());
      if (manager.getDynamicPropertiesStore().getAllowMultiSign() != 1) {
        flag = Arrays.equals(sigAddress, msg.getAddress().toByteArray());
      } else {
        byte[] witnessPermissionAddress = manager.getAccountStore()
            .get(msg.getAddress().toByteArray()).getWitnessPermissionAddress();
        flag = Arrays.equals(sigAddress, witnessPermissionAddress);
      }
      if (flag) {
        TronNetService.getP2pConfig().getTrustNodes().add(channel.getInetAddress());
      }
      return flag;
    } catch (Exception e) {
      logger.error("Check hello message failed, msg: {}, {}", message, channel.getInetAddress(), e);
      return false;
    }
  }

  private boolean isActiveWitness() {
    return parameter.isWitness()
        && keySize > 0
        && fastForwardNodes.size() > 0
        && witnessScheduleStore.getActiveWitnesses().contains(witnessAddress)
        && backupManager.getStatus().equals(BackupStatusEnum.MASTER);
  }

  private void connect() {
    for (InetSocketAddress fastForwardNode : fastForwardNodes) {
      if (!TronNetService.getP2pConfig().getActiveNodes().contains(fastForwardNode)) {
        TronNetService.getP2pConfig().getActiveNodes().add(fastForwardNode);
      }
    }
  }

  private void disconnect() {
    fastForwardNodes.forEach(address -> {
      TronNetService.getP2pConfig().getActiveNodes().remove(address);
      TronNetService.getPeers().forEach(peer -> {
        if (peer.getInetAddress().equals(address.getAddress())) {
          peer.getChannel().close();
        }
      });
    });
  }

  private Set<ByteString> getNextWitnesses(ByteString key, Integer count) {
    List<ByteString> list = chainBaseManager.getWitnessScheduleStore().getActiveWitnesses();
    int index = list.indexOf(key);
    if (index < 0) {
      return new HashSet<>(list);
    }
    Set<ByteString> set = new HashSet<>();
    for (; count > 0; count--) {
      set.add(list.get(++index % list.size()));
    }
    return set;
  }

  public void broadcast(BlockMessage msg) {
    Set<ByteString> witnesses = getNextWitnesses(
            msg.getBlockCapsule().getWitnessAddress(), maxFastForwardNum);
    Item item = new Item(msg.getBlockId(), Protocol.Inventory.InventoryType.BLOCK);
    List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
            .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
            .filter(peer -> peer.getAdvInvReceive().getIfPresent(item) == null
                    && peer.getAdvInvSpread().getIfPresent(item) == null)
            .filter(peer -> peer.getAddress() != null && witnesses.contains(peer.getAddress()))
            .collect(Collectors.toList());

    peers.forEach(peer -> {
      peer.sendMessage(msg);
      peer.getAdvInvSpread().put(item, System.currentTimeMillis());
      peer.setFastForwardBlock(msg.getBlockId());
    });
  }
}
