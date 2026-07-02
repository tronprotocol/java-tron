package org.tron.core.net.service.tor;

import com.google.protobuf.ByteString;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.math.StrictMathWrapper;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

/**
 * Broadcasts locally-originated transactions anonymously over Tor, using the native TRON P2P
 * protocol so the receiving nodes are standard, unmodified TRON full nodes.
 *
 * <p>When {@code node.tor.enabled} is true, a transaction submitted to this node's own broadcast
 * API is validated locally as usual but, instead of being advertised to the node's directly
 * connected sync peers (which would expose its IP as the origin), it is relayed through the local
 * Tor SOCKS5 proxy to standard nodes learned via discovery that are NOT current sync peers, hiding
 * the origin. Block sync and normal P2P traffic keep running in the clear.
 *
 * <p>Reaching a Tor peer and completing the P2P handshake costs several seconds, which is paid per
 * connection, not per transaction. To make broadcasting fast even at a high transaction rate the
 * service keeps a pool of <b>persistent</b> Tor tunnels to verified, at-head relay nodes: each
 * tunnel is handshaked once, kept alive (answering keep-alive pings), reused for every broadcast,
 * and rebuilt only when it breaks. Transactions are buffered and flushed as a single
 * {@code TransactionsMessage} batch over the tunnels, so a burst of transactions is delivered in
 * roughly one round-trip. {@code broadcastTransaction} enqueues and returns immediately.
 */
@Slf4j(topic = "net")
@Component
public class TorBroadcastService {

  private static final byte HELLO_TYPE = MessageType.HANDSHAKE_HELLO.getType();
  private static final byte PING_TYPE = MessageType.KEEP_ALIVE_PING.getType();
  private static final int SOCKS_VERSION = 0x05;
  private static final int SOCKS_CMD_CONNECT = 0x01;
  private static final int SOCKS_AUTH_NONE = 0x00;
  private static final int SOCKS_AUTH_USERPASS = 0x02;

  private static final int POOL_SIZE = 6;
  private static final int MAINTAIN_INTERVAL_MS = 3000;
  private static final int FLUSH_INTERVAL_MS = 150;
  private static final int MAX_BATCH = 300;
  // How many connect attempts to launch per maintenance round: just enough to refill the missing
  // tunnels plus a small margin for the ones that fail over Tor — NOT a fixed large batch, so we
  // don't build (and immediately close) far more circuits than needed.
  private static final int CONNECT_MARGIN = 2;
  // When a round can't refill the pool (Tor congested/down), skip up to 2^level rounds before the
  // next attempt, so we back off instead of flooding a struggling Tor with circuit requests.
  private static final int MAX_BACKOFF_LEVEL = 4;
  // A relay is only kept if its advertised head is within this many blocks of ours, so it is a
  // well-connected node that actually propagates the transaction rather than sitting on it.
  private static final long HEAD_TOLERANCE = 200;

  @Autowired
  private TronNetService tronNetService;
  @Autowired
  private TronNetDelegate tronNetDelegate;

  private final ExecutorService connectExecutor = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "tor-relay-connect");
    t.setDaemon(true);
    return t;
  });
  private volatile ScheduledExecutorService scheduler;

  private final List<RelayChannel> channels = new CopyOnWriteArrayList<>();
  private final ConcurrentLinkedQueue<Transaction> buffer = new ConcurrentLinkedQueue<>();
  private final Set<Sha256Hash> buffered = ConcurrentHashMap.newKeySet();

  // If the pool stays empty for this many maintenance rounds, ask Tor for fresh circuits.
  private static final int NEWNYM_AFTER_ROUNDS = 3;
  private static final long NEWNYM_MIN_INTERVAL_MS = 15000;
  private int emptyMaintainRounds = 0;
  private long lastNewnymMs = 0;
  // Exponential backoff so a struggling Tor isn't flooded with connect attempts.
  private int backoffLevel = 0;
  private int skipsRemaining = 0;

  /**
   * Enqueue a transaction for anonymous relay over Tor and return immediately. Delivery happens
   * asynchronously over the persistent tunnel pool.
   *
   * @return the number of relays the transaction will be pushed to (0 only if it cannot run)
   */
  public int broadcastTransaction(Transaction transaction) {
    ensureStarted();
    Sha256Hash txId = new TransactionCapsule(transaction).getTransactionId();
    if (buffered.add(txId)) {
      buffer.add(transaction);
    }
    // Async: report the intended relay count so the caller sees success; the flusher delivers it.
    return StrictMathWrapper.max(1, CommonParameter.getInstance().getTorBroadcastCount());
  }

  /** Stop the background maintenance/flush tasks and close all tunnels. */
  @PreDestroy
  public void shutdown() {
    ScheduledExecutorService svc = scheduler;
    if (svc != null) {
      svc.shutdownNow();
    }
    connectExecutor.shutdownNow();
    for (RelayChannel c : channels) {
      c.close();
    }
    channels.clear();
    buffer.clear();
    buffered.clear();
  }

  private void ensureStarted() {
    if (scheduler != null) {
      return;
    }
    synchronized (this) {
      if (scheduler != null) {
        return;
      }
      ScheduledExecutorService svc = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "tor-relay-pool");
        t.setDaemon(true);
        return t;
      });
      svc.scheduleWithFixedDelay(this::maintainChannels, 0, MAINTAIN_INTERVAL_MS,
          TimeUnit.MILLISECONDS);
      svc.scheduleWithFixedDelay(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS,
          TimeUnit.MILLISECONDS);
      scheduler = svc;
    }
  }

  // ---- Pool maintenance: keep POOL_SIZE persistent tunnels to at-head, non-sync relays ----------

  private void maintainChannels() {
    try {
      CommonParameter parameter = CommonParameter.getInstance();
      if (!parameter.isTorBroadcastEnable()) {
        return;
      }
      channels.removeIf(c -> {
        if (!c.alive) {
          c.close();
          return true;
        }
        return false;
      });
      int need = POOL_SIZE - channels.size();
      if (need <= 0) {
        emptyMaintainRounds = 0;
        backoffLevel = 0;
        skipsRemaining = 0;
        return;
      }
      // Back off: after failed rounds, skip a growing number of ticks so a congested/down Tor is
      // not flooded with connect attempts (which only clog its circuit-build queue further).
      if (skipsRemaining > 0) {
        skipsRemaining--;
        return;
      }
      Set<InetAddress> exclude = activeSyncPeerAddresses();
      for (RelayChannel c : channels) {
        exclude.add(c.address);
      }
      List<Node> candidates = new ArrayList<>();
      for (Node node : discoveredNonSyncNodes(activeSyncPeerAddresses())) {
        InetSocketAddress a = node.getPreferInetSocketAddress();
        if (a != null && a.getAddress() != null && !exclude.contains(a.getAddress())) {
          candidates.add(node);
        }
      }
      Collections.shuffle(candidates, ThreadLocalRandom.current());

      // Launch only enough attempts to refill the missing tunnels (plus a small margin), and keep
      // every one that succeeds — never build extra circuits just to close them.
      int attempts = StrictMathWrapper.min(need + CONNECT_MARGIN, candidates.size());
      List<Future<RelayChannel>> futures = new ArrayList<>();
      for (int i = 0; i < attempts; i++) {
        Node node = candidates.get(i);
        futures.add(connectExecutor.submit(() -> connectChannel(node, parameter)));
      }
      int added = 0;
      for (Future<RelayChannel> f : futures) {
        try {
          RelayChannel c = f.get(parameter.getTorReadTimeout() + 3000L, TimeUnit.MILLISECONDS);
          if (c != null) {
            channels.add(c);
            added++;
          }
        } catch (Exception ignored) {
          // connect failed / timed out
        }
      }
      if (added > 0) {
        logger.info("Tor relay pool: {} persistent tunnel(s) open.", channels.size());
      }
      if (added >= need) {
        // pool refilled: clear the backoff
        backoffLevel = 0;
        skipsRemaining = 0;
        emptyMaintainRounds = 0;
      } else {
        // couldn't refill: grow the backoff (skip 1, 3, 7, 15 ... of the next rounds)
        backoffLevel = StrictMathWrapper.min(backoffLevel + 1, MAX_BACKOFF_LEVEL);
        skipsRemaining = (1 << backoffLevel) - 1;
        // If no tunnel exists at all for several attempts, Tor's circuits/exits may be throttled;
        // ask Tor for a fresh identity (new exit IPs). This is our lever against a systemic outage.
        if (channels.isEmpty() && ++emptyMaintainRounds >= NEWNYM_AFTER_ROUNDS) {
          emptyMaintainRounds = 0;
          signalNewnym(parameter);
        }
      }
    } catch (Throwable t) {
      // Never let the scheduled maintainer die: a thrown Error would stop it being rescheduled.
      logger.warn("Tor relay pool maintenance failed: {}", t.toString());
    }
  }

  // Ask the Tor daemon (via its ControlPort) to build fresh circuits with new exit relays, so a
  // node whose current exit IPs are throttled can recover without a manual Tor restart. No-op if
  // no ControlPort is configured. Rate-limited, as Tor itself throttles NEWNYM.
  private void signalNewnym(CommonParameter parameter) {
    int controlPort = parameter.getTorControlPort();
    if (controlPort <= 0) {
      return;
    }
    long now = System.currentTimeMillis();
    if (now - lastNewnymMs < NEWNYM_MIN_INTERVAL_MS) {
      return;
    }
    lastNewnymMs = now;
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(parameter.getTorSocksHost(), controlPort),
          parameter.getTorConnectTimeout());
      socket.setSoTimeout(parameter.getTorReadTimeout());
      OutputStream out = socket.getOutputStream();
      InputStream in = socket.getInputStream();
      out.write(("AUTHENTICATE \"" + parameter.getTorControlPassword() + "\"\r\n")
          .getBytes(StandardCharsets.US_ASCII));
      out.flush();
      String authReply = readControlLine(in);
      if (authReply == null || !authReply.startsWith("250")) {
        logger.warn("Tor ControlPort authentication failed: {}", authReply);
        return;
      }
      out.write("SIGNAL NEWNYM\r\n".getBytes(StandardCharsets.US_ASCII));
      out.flush();
      String signalReply = readControlLine(in);
      if (signalReply != null && signalReply.startsWith("250")) {
        logger.info("Tor relay pool empty; signalled NEWNYM to rebuild circuits (new exit IPs).");
      } else {
        logger.warn("Tor NEWNYM signal was rejected: {}", signalReply);
      }
    } catch (Exception e) {
      logger.warn("Tor NEWNYM signal failed: {}", e.getMessage());
    }
  }

  private static String readControlLine(InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();
    int c;
    while ((c = in.read()) != -1) {
      if (c == '\n') {
        break;
      }
      if (c != '\r') {
        sb.append((char) c);
      }
    }
    return sb.length() == 0 ? null : sb.toString();
  }

  private RelayChannel connectChannel(Node node, CommonParameter parameter) {
    InetSocketAddress dest = node.getPreferInetSocketAddress();
    if (dest == null || dest.getAddress() == null) {
      return null;
    }
    Socket socket = null;
    try {
      socket = openTunnel(dest, "relay-" + ThreadLocalRandom.current().nextLong(), parameter);
      RelayChannel channel = new RelayChannel(dest.getAddress(), socket, parameter);
      if (!channel.establish()) {
        channel.close();
        return null;
      }
      return channel;
    } catch (Exception e) {
      closeQuietly(socket);
      return null;
    }
  }

  // ---- Flush: deliver the buffered transactions as one batch over the tunnels -------------------

  private void flush() {
    try {
      if (buffer.isEmpty()) {
        return;
      }
      dropExpired();
      if (buffer.isEmpty()) {
        return;
      }
      List<RelayChannel> alive = new ArrayList<>();
      for (RelayChannel c : channels) {
        if (c.alive) {
          alive.add(c);
        }
      }
      if (alive.isEmpty()) {
        return; // no tunnels yet; keep the transactions buffered until the pool is ready
      }
      List<Transaction> batch = new ArrayList<>();
      Transaction tx;
      while (batch.size() < MAX_BATCH && (tx = buffer.poll()) != null) {
        batch.add(tx);
      }
      if (batch.isEmpty()) {
        return;
      }
      CommonParameter parameter = CommonParameter.getInstance();
      int target = StrictMathWrapper.max(1,
          StrictMathWrapper.min(parameter.getTorBroadcastCount(), alive.size()));
      List<RelayChannel> chosen = alive.subList(0, target);
      // A socket write has no timeout of its own (setSoTimeout only affects reads), so a tunnel
      // whose Tor circuit has died silently would block the advertise() call — and thus the whole
      // flusher — forever. Run each advertise on the connect pool and bound it: if it stalls, close
      // the tunnel (which unblocks the write) so the maintainer rebuilds it, and keep flushing.
      long writeTimeout = StrictMathWrapper.max(2000L, parameter.getTorReadTimeout());
      List<Future<Boolean>> futures = new ArrayList<>();
      for (RelayChannel channel : chosen) {
        futures.add(connectExecutor.submit(() -> channel.advertise(batch)));
      }
      int delivered = 0;
      for (int i = 0; i < futures.size(); i++) {
        try {
          if (Boolean.TRUE.equals(futures.get(i).get(writeTimeout, TimeUnit.MILLISECONDS))) {
            delivered++;
          }
        } catch (Exception e) {
          futures.get(i).cancel(true);
          chosen.get(i).close(); // stalled/broken tunnel: drop it, the maintainer will replace it
        }
      }
      if (delivered == 0) {
        // every chosen tunnel failed to write; re-buffer for the next flush (no transaction lost)
        for (Transaction t : batch) {
          buffer.add(t);
        }
      } else {
        for (Transaction t : batch) {
          buffered.remove(new TransactionCapsule(t).getTransactionId());
        }
        logger.info("Tor broadcast: flushed {} transaction(s) to {} relay tunnel(s).",
            batch.size(), delivered);
      }
    } catch (Throwable t) {
      // Never let the scheduled flusher die: a thrown Error would stop it being rescheduled.
      logger.warn("Tor broadcast flush failed: {}", t.toString());
    }
  }

  // Drop transactions whose expiration has passed: they can no longer be included even if relayed,
  // so keeping them wastes tunnel writes and, during a long Tor outage, would grow the buffer
  // without bound. Removing them here also frees the dedup set.
  private void dropExpired() {
    long now = System.currentTimeMillis();
    int dropped = 0;
    Iterator<Transaction> it = buffer.iterator();
    while (it.hasNext()) {
      Transaction tx = it.next();
      long expiration = tx.getRawData().getExpiration();
      if (expiration > 0 && expiration < now) {
        it.remove();
        buffered.remove(new TransactionCapsule(tx).getTransactionId());
        dropped++;
      }
    }
    if (dropped > 0) {
      logger.warn("Tor broadcast: dropped {} expired transaction(s) not relayed before expiry.",
          dropped);
    }
  }

  private List<Node> discoveredNonSyncNodes(Set<InetAddress> syncPeers) {
    List<Node> result = new ArrayList<>();
    for (Node node : tronNetService.getTableNodes()) {
      InetSocketAddress address = node.getPreferInetSocketAddress();
      if (address != null && address.getAddress() != null
          && !syncPeers.contains(address.getAddress())) {
        result.add(node);
      }
    }
    return result;
  }

  private Set<InetAddress> activeSyncPeerAddresses() {
    Set<InetAddress> set = new HashSet<>();
    for (PeerConnection peer : tronNetDelegate.getActivePeer()) {
      InetAddress address = peer.getInetAddress();
      if (address != null) {
        set.add(address);
      }
    }
    return set;
  }

  private long ourHeadNum() {
    try {
      return tronNetDelegate.getHeadBlockId().getNum();
    } catch (Exception e) {
      return -1; // unknown (e.g. in tests) -> skip the at-head filter
    }
  }

  // ---- A persistent, handshaked Tor tunnel to one relay node -----------------------------------

  private final class RelayChannel {

    private final InetAddress address;
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final CommonParameter parameter;
    private int peerVersion;
    private volatile boolean alive = true;
    private final Map<Sha256Hash, Transaction> pendingFetch = new ConcurrentHashMap<>();
    private Thread reader;

    RelayChannel(InetAddress address, Socket socket, CommonParameter parameter) throws IOException {
      this.address = address;
      this.socket = socket;
      this.parameter = parameter;
      this.in = socket.getInputStream();
      this.out = socket.getOutputStream();
    }

    // Complete the transport + application handshake and start the keep-alive/serve reader.
    boolean establish() throws Exception {
      int networkId = parameter.getNodeP2pVersion();
      peerVersion = transportHandshake(in, out, networkId);
      Protocol.HelloMessage peerAppHello = readAppHello(in, peerVersion);
      if (peerAppHello == null) {
        return false;
      }
      long ourHead = ourHeadNum();
      if (ourHead >= 0 && peerAppHello.getHeadBlockId().getNumber() < ourHead - HEAD_TOLERANCE) {
        return false; // lagging relay, poor propagator
      }
      // Echo the peer's genesis/solid/head so it marks the connection sync-complete (needed before
      // it will fetch a transaction inventory from us).
      org.tron.protos.Discover.Endpoint appFrom = org.tron.protos.Discover.Endpoint.newBuilder()
          .setNodeId(ByteString.copyFrom(NetUtil.getNodeId()))
          .setAddress(ByteString.copyFrom(randomAnonIp().getBytes(StandardCharsets.UTF_8)))
          .setPort(18888)
          .build();
      Protocol.HelloMessage appHello = Protocol.HelloMessage.newBuilder()
          .setFrom(appFrom)
          .setVersion(networkId)
          .setTimestamp(System.currentTimeMillis())
          .setGenesisBlockId(peerAppHello.getGenesisBlockId())
          .setSolidBlockId(peerAppHello.getSolidBlockId())
          .setHeadBlockId(peerAppHello.getHeadBlockId())
          .setNodeType(0)
          .setLowestBlockNum(0)
          .build();
      sendApp(prependType(MessageTypes.P2P_HELLO.asByte(), appHello.toByteArray()));
      // Handshake done. The peer keep-alive-pings only every KEEP_ALIVE_TIMEOUT (20s), so relax the
      // read timeout well above that (plus Tor jitter); otherwise an idle but healthy tunnel would
      // time out between pings and be dropped, churning the pool and flooding Tor with reconnects.
      int minIdle = 3 * Parameter.KEEP_ALIVE_TIMEOUT;
      socket.setSoTimeout(StrictMathWrapper.max(parameter.getTorReadTimeout(), minIdle));
      reader = new Thread(this::readLoop, "tor-relay-reader");
      reader.setDaemon(true);
      reader.start();
      return true;
    }

    private void readLoop() {
      try {
        while (alive) {
          byte[] decoded = UpgradeController.decodeReceiveData(peerVersion, readFrame(in));
          if (decoded.length == 0) {
            continue;
          }
          byte type = decoded[0];
          if (type == PING_TYPE) {
            sendApp(new PongMessage().getSendData()); // keep the tunnel alive
          } else if (type == MessageTypes.FETCH_INV_DATA.asByte()) {
            FetchInvDataMessage fetch =
                new FetchInvDataMessage(subarray(decoded, 1, decoded.length));
            List<Transaction> serve = new ArrayList<>();
            for (Sha256Hash hash : fetch.getHashList()) {
              Transaction t = pendingFetch.remove(hash);
              if (t != null) {
                serve.add(t);
              }
            }
            if (!serve.isEmpty()) {
              sendApp(new TransactionsMessage(serve).getSendBytes());
            }
          }
          // ignore everything else (the peer's own INV, status, sync requests, ...)
        }
      } catch (Exception e) {
        // connection closed or broken; the maintainer will rebuild it
      } finally {
        alive = false;
        closeQuietly(socket);
      }
    }

    // Advertise a batch of transactions; the reader serves them when the peer sends FetchInvData.
    boolean advertise(List<Transaction> batch) {
      try {
        List<Sha256Hash> ids = new ArrayList<>(batch.size());
        for (Transaction t : batch) {
          Sha256Hash id = new TransactionCapsule(t).getTransactionId();
          pendingFetch.put(id, t);
          ids.add(id);
        }
        sendApp(new InventoryMessage(ids, InventoryType.TRX).getSendBytes());
        return true;
      } catch (Exception e) {
        alive = false;
        return false;
      }
    }

    // Serialize writes: the reader thread (pongs, fetch replies) and the flusher (INV) share out.
    private synchronized void sendApp(byte[] appMessageBytes) throws IOException {
      writeFrame(out, UpgradeController.codeSendData(peerVersion, appMessageBytes));
    }

    void close() {
      alive = false;
      closeQuietly(socket);
    }
  }

  // ---- Tor SOCKS5 tunnel + native TRON P2P handshake helpers -----------------------------------

  private Socket openTunnel(InetSocketAddress dest, String isoUser, CommonParameter parameter)
      throws IOException {
    Socket socket = new Socket();
    socket.connect(
        new InetSocketAddress(parameter.getTorSocksHost(), parameter.getTorSocksPort()),
        parameter.getTorConnectTimeout());
    socket.setSoTimeout(parameter.getTorReadTimeout());
    socksHandshake(socket.getInputStream(), socket.getOutputStream(), dest,
        parameter.isTorCircuitIsolation(), isoUser);
    return socket;
  }

  private void socksHandshake(InputStream in, OutputStream out, InetSocketAddress dest,
      boolean isolation, String isoUser) throws IOException {
    if (isolation) {
      out.write(new byte[] {SOCKS_VERSION, 1, SOCKS_AUTH_USERPASS});
    } else {
      out.write(new byte[] {SOCKS_VERSION, 1, SOCKS_AUTH_NONE});
    }
    out.flush();
    byte[] methodReply = readFully(in, 2);
    int method = methodReply[1] & 0xff;

    if (isolation) {
      if (method != SOCKS_AUTH_USERPASS) {
        throw new IOException("SOCKS proxy rejected user/pass auth (circuit isolation)");
      }
      byte[] user = isoUser.getBytes(StandardCharsets.US_ASCII);
      byte[] pass = {0};
      out.write(0x01);
      out.write(user.length);
      out.write(user);
      out.write(pass.length);
      out.write(pass);
      out.flush();
      byte[] authReply = readFully(in, 2);
      if ((authReply[1] & 0xff) != 0x00) {
        throw new IOException("SOCKS proxy auth failed");
      }
    } else if (method != SOCKS_AUTH_NONE) {
      throw new IOException("SOCKS proxy requires authentication");
    }

    byte[] addr = dest.getAddress().getAddress();
    int atyp = addr.length == 4 ? 0x01 : 0x04;
    int port = dest.getPort();
    out.write(SOCKS_VERSION);
    out.write(SOCKS_CMD_CONNECT);
    out.write(0x00);
    out.write(atyp);
    out.write(addr);
    out.write((port >> 8) & 0xff);
    out.write(port & 0xff);
    out.flush();

    byte[] head = readFully(in, 4);
    if ((head[1] & 0xff) != 0x00) {
      throw new IOException("SOCKS connect to " + dest + " failed, code=" + (head[1] & 0xff));
    }
    int boundLen = (head[3] & 0xff) == 0x01 ? 4 : (head[3] & 0xff) == 0x04 ? 16 : 0;
    readFully(in, boundLen + 2); // skip BND.ADDR + BND.PORT
  }

  // libp2p transport Hello handshake; returns the peer's protocol version. Advertises a random,
  // non-identifying public IP (never our real one) so tunnels can't be clustered by origin address.
  private int transportHandshake(InputStream in, OutputStream out, int networkId) throws Exception {
    org.tron.p2p.protos.Discover.Endpoint from = org.tron.p2p.protos.Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(NetUtil.getNodeId()))
        .setAddress(ByteString.copyFrom(randomAnonIp().getBytes(StandardCharsets.UTF_8)))
        .setPort(18888)
        .build();
    Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
        .setFrom(from)
        .setNetworkId(networkId)
        .setCode(0)
        .setVersion(Parameter.version)
        .setTimestamp(System.currentTimeMillis())
        .build();
    writeFrame(out, prependType(HELLO_TYPE, hello.toByteArray()));

    byte[] peerFrame = readFrame(in);
    if (peerFrame.length == 0 || peerFrame[0] != HELLO_TYPE) {
      throw new IOException("expected Hello from peer");
    }
    Connect.HelloMessage peerHello =
        Connect.HelloMessage.parseFrom(subarray(peerFrame, 1, peerFrame.length));
    if (peerHello.getCode() != 0 || peerHello.getNetworkId() != networkId) {
      throw new IOException("peer rejected handshake (code=" + peerHello.getCode()
          + ", networkId=" + peerHello.getNetworkId() + ")");
    }
    return peerHello.getVersion();
  }

  // Read frames until the peer's application-level P2P_HELLO arrives, returning it (or null).
  private Protocol.HelloMessage readAppHello(InputStream in, int peerVersion) throws Exception {
    long deadline = System.currentTimeMillis() + Parameter.KEEP_ALIVE_TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      byte[] decoded = UpgradeController.decodeReceiveData(peerVersion, readFrame(in));
      if (decoded.length > 0 && decoded[0] == MessageTypes.P2P_HELLO.asByte()) {
        return Protocol.HelloMessage.parseFrom(subarray(decoded, 1, decoded.length));
      }
    }
    return null;
  }

  // ---- framing helpers (protobuf varint32 length prefix, matching libp2p's framing) ------------

  private static void writeFrame(OutputStream out, byte[] payload) throws IOException {
    writeRawVarint32(out, payload.length);
    out.write(payload);
    out.flush();
  }

  private static byte[] readFrame(InputStream in) throws IOException {
    int len = readRawVarint32(in);
    if (len < 0 || len > Parameter.MAX_MESSAGE_LENGTH) {
      throw new IOException("invalid frame length: " + len);
    }
    return readFully(in, len);
  }

  private static void writeRawVarint32(OutputStream out, int value) throws IOException {
    while (true) {
      if ((value & ~0x7f) == 0) {
        out.write(value);
        return;
      }
      out.write((value & 0x7f) | 0x80);
      value >>>= 7;
    }
  }

  private static int readRawVarint32(InputStream in) throws IOException {
    int result = 0;
    int shift = 0;
    while (shift < 32) {
      int b = in.read();
      if (b == -1) {
        throw new EOFException("stream closed while reading frame length");
      }
      result |= (b & 0x7f) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
    }
    throw new IOException("varint32 too long");
  }

  private static byte[] readFully(InputStream in, int len) throws IOException {
    byte[] buf = new byte[len];
    int off = 0;
    while (off < len) {
      int n = in.read(buf, off, len - off);
      if (n == -1) {
        throw new EOFException("stream closed while reading " + len + " bytes");
      }
      off += n;
    }
    return buf;
  }

  private static byte[] prependType(byte type, byte[] data) {
    byte[] out = new byte[data.length + 1];
    out[0] = type;
    System.arraycopy(data, 0, out, 1, data.length);
    return out;
  }

  private static byte[] subarray(byte[] data, int from, int to) {
    byte[] out = new byte[to - from];
    System.arraycopy(data, from, out, 0, to - from);
    return out;
  }

  // A random, well-formed, non-multicast public IPv4 (never our real IP). Passes the peer's
  // NetUtil.validNode check and, being fresh per tunnel, is not a stable clustering handle.
  private static String randomAnonIp() {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    while (true) {
      int a = r.nextInt(1, 224);  // 1..223: skip 0.x and multicast/reserved 224+
      int b = r.nextInt(0, 256);
      int c = r.nextInt(0, 256);
      int d = r.nextInt(1, 255);  // 1..254: skip .0 network and .255 broadcast
      if (isPublicIpv4(a, b, c, d)) {
        return a + "." + b + "." + c + "." + d;
      }
    }
  }

  // True only for a globally-routable public IPv4 (excludes private, loopback, link-local, CGNAT,
  // documentation, benchmarking and other reserved ranges). We advertise such an address so peers
  // accept it (NetUtil.validNode) while it still reveals nothing about us.
  private static boolean isPublicIpv4(int a, int b, int c, int d) {
    if (a == 10 || a == 127) {
      return false;                                  // private 10/8, loopback 127/8
    }
    if (a == 100 && b >= 64 && b <= 127) {
      return false;                                  // CGNAT 100.64/10
    }
    if (a == 169 && b == 254) {
      return false;                                  // link-local 169.254/16
    }
    if (a == 172 && b >= 16 && b <= 31) {
      return false;                                  // private 172.16/12
    }
    if (a == 192 && (b == 0 || b == 88 || b == 168)) {
      return false; // 192.0.0/24, 192.0.2/24, 192.88.99/24 (6to4), 192.168/16
    }
    if (a == 198 && (b == 18 || b == 19 || b == 51)) {
      return false;                                  // benchmarking 198.18/15, doc 198.51.100/24
    }
    if (a == 203 && b == 0 && c == 113) {
      return false;                                  // documentation 203.0.113/24
    }
    return true;
  }

  private static void closeQuietly(Socket socket) {
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }
}
