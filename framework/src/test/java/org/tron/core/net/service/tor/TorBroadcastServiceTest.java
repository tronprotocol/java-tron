package org.tron.core.net.service.tor;

import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

/**
 * End-to-end test for {@link TorBroadcastService}: an enqueued transaction must reach a standard
 * TRON node over a persistent tunnel using the native P2P protocol (Hello -&gt; P2P_HELLO -&gt; INV
 * -&gt; FetchInvData -&gt; TransactionsMessage), tunnelled through a SOCKS5 proxy. A minimal
 * in-process SOCKS5 forwarder stands in for Tor, and a stub node speaks just enough of the wire
 * protocol to accept the transaction — so the whole routing + framing + compression path runs
 * without any external dependency. Delivery is asynchronous (buffer + background flush), so the
 * test waits for the stub to receive the transaction.
 */
public class TorBroadcastServiceTest {

  private static final int NETWORK_ID = 11111;
  private static final byte HELLO = (byte) 0xfd;

  private StubNode stubNode;
  private Socks5Forwarder socksProxy;
  private TorBroadcastService service;
  private final AtomicBoolean transactionReceived = new AtomicBoolean(false);

  @BeforeClass
  public static void initArgs() {
    // Initialise CommonParameter as the other net tests do (also sets up global state this test's
    // service touches). PeerConnection's static relayNodes is safe regardless of ordering now that
    // CommonParameter.fastForwardNodes defaults to an empty list.
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
  }

  @Before
  public void setUp() throws IOException {
    if (Parameter.p2pConfig == null) {
      Parameter.p2pConfig = new org.tron.p2p.P2pConfig();
    }
    stubNode = new StubNode(transactionReceived);
    stubNode.start();
    socksProxy = new Socks5Forwarder();
    socksProxy.start();

    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setTorSocksHost("127.0.0.1");
    parameter.setTorSocksPort(socksProxy.getPort());
    parameter.setTorConnectTimeout(5000);
    parameter.setTorReadTimeout(5000);
    parameter.setTorBroadcastEnable(true);
    parameter.setTorCircuitIsolation(false); // keep the in-process forwarder auth-free
    parameter.setTorBroadcastCount(1);
    parameter.setTorControlPort(0);
    parameter.setNodeP2pVersion(NETWORK_ID);
  }

  @After
  public void tearDown() {
    if (service != null) {
      service.shutdown(); // stop the background threads so they don't leak into other tests
      service = null;
    }
    if (stubNode != null) {
      stubNode.close();
    }
    if (socksProxy != null) {
      socksProxy.close();
    }
  }

  @Test
  public void transactionDeliveredOverPersistentTunnel() throws Exception {
    Node stub = new Node(new InetSocketAddress("127.0.0.1", stubNode.getPort()));
    newService(Collections.singletonList(stub));

    service.broadcastTransaction(sampleTransaction());

    // Delivery is async (pool maintenance + flush run in the background); wait for the stub.
    long deadline = System.currentTimeMillis() + 15000;
    while (System.currentTimeMillis() < deadline && !transactionReceived.get()) {
      Thread.sleep(100);
    }
    Assert.assertTrue("stub node never received the transaction over the persistent tunnel",
        transactionReceived.get());
  }

  @Test
  public void noRelaysNothingDelivered() throws Exception {
    newService(Collections.emptyList());

    service.broadcastTransaction(sampleTransaction());

    Thread.sleep(2000);
    Assert.assertFalse("nothing should be delivered when there are no relay nodes",
        transactionReceived.get());
  }

  @Test
  public void torProxyOutageThenRecovery() throws Exception {
    // Point the SOCKS proxy at a closed port: the Tor daemon is "down".
    CommonParameter.getInstance().setTorSocksPort(freePort());
    Node stub = new Node(new InetSocketAddress("127.0.0.1", stubNode.getPort()));
    newService(Collections.singletonList(stub));

    service.broadcastTransaction(sampleTransaction());
    Thread.sleep(2500);
    Assert.assertFalse("nothing should be delivered while Tor is down",
        transactionReceived.get());

    // Tor comes back: point the proxy at the working forwarder. No transaction is lost — the
    // buffered one must be delivered once tunnels can be built again.
    CommonParameter.getInstance().setTorSocksPort(socksProxy.getPort());
    waitUntil(transactionReceived, 30000);
    Assert.assertTrue("buffered transaction must be delivered once Tor recovers",
        transactionReceived.get());
  }

  @Test
  public void relayDegradedThenRecovers() throws Exception {
    stubNode.healthy = false; // the only relay drops us during the handshake
    Node stub = new Node(new InetSocketAddress("127.0.0.1", stubNode.getPort()));
    newService(Collections.singletonList(stub));

    service.broadcastTransaction(sampleTransaction());
    Thread.sleep(2500);
    Assert.assertFalse("nothing should be delivered while the only relay is degraded",
        transactionReceived.get());

    // Relay recovers: the pool must rebuild a tunnel and deliver the buffered transaction.
    stubNode.healthy = true;
    waitUntil(transactionReceived, 30000);
    Assert.assertTrue("buffered transaction must be delivered once the relay recovers",
        transactionReceived.get());
  }

  @Test
  public void newnymSignalledWhenPoolCannotForm() throws Exception {
    TorControlServer control = new TorControlServer();
    control.start();
    CommonParameter.getInstance().setTorControlPort(control.getPort());
    CommonParameter.getInstance().setTorControlPassword("");
    try {
      // No relay nodes -> the pool can never form -> after a few empty rounds the node should ask
      // Tor for fresh circuits (new exit IPs) via the control port.
      newService(Collections.emptyList());
      service.broadcastTransaction(sampleTransaction());
      waitUntil(control.newnymReceived, 40000);
      Assert.assertTrue("node should signal NEWNYM when no relay tunnel can be built",
          control.newnymReceived.get());
    } finally {
      CommonParameter.getInstance().setTorControlPort(0);
      control.close();
    }
  }

  private static void waitUntil(AtomicBoolean flag, long timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline && !flag.get()) {
      Thread.sleep(100);
    }
  }

  private static int freePort() throws IOException {
    try (ServerSocket s = new ServerSocket()) {
      s.bind(new InetSocketAddress("127.0.0.1", 0));
      return s.getLocalPort();
    }
  }

  private TorBroadcastService newService(java.util.List<Node> tableNodes) {
    TorBroadcastService svc = new TorBroadcastService();
    TronNetService netService = Mockito.mock(TronNetService.class);
    TronNetDelegate netDelegate = Mockito.mock(TronNetDelegate.class);
    Mockito.when(netService.getTableNodes()).thenReturn(tableNodes);
    Mockito.when(netDelegate.getActivePeer()).thenReturn(Collections.emptyList());
    ReflectionTestUtils.setField(svc, "tronNetService", netService);
    ReflectionTestUtils.setField(svc, "tronNetDelegate", netDelegate);
    this.service = svc; // tracked so tearDown can shut it down
    return svc;
  }

  private static Transaction sampleTransaction() {
    return Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().setTimestamp(1L))
        .build();
  }

  // ---- framing helpers (mirror the service / libp2p wire format) ------------------------------

  private static void writeFrame(OutputStream out, byte[] payload) throws IOException {
    int value = payload.length;
    while (true) {
      if ((value & ~0x7f) == 0) {
        out.write(value);
        break;
      }
      out.write((value & 0x7f) | 0x80);
      value >>>= 7;
    }
    out.write(payload);
    out.flush();
  }

  private static byte[] readFrame(InputStream in) throws IOException {
    int result = 0;
    int shift = 0;
    while (shift < 32) {
      int b = in.read();
      if (b == -1) {
        throw new IOException("closed");
      }
      result |= (b & 0x7f) << shift;
      if ((b & 0x80) == 0) {
        break;
      }
      shift += 7;
    }
    byte[] buf = new byte[result];
    int off = 0;
    while (off < result) {
      int n = in.read(buf, off, result - off);
      if (n == -1) {
        throw new IOException("closed");
      }
      off += n;
    }
    return buf;
  }

  private static byte[] prepend(byte type, byte[] data) {
    byte[] out = new byte[data.length + 1];
    out[0] = type;
    System.arraycopy(data, 0, out, 1, data.length);
    return out;
  }

  private static byte[] tail(byte[] data) {
    byte[] out = new byte[data.length - 1];
    System.arraycopy(data, 1, out, 0, out.length);
    return out;
  }

  /** A stub standard TRON node: handshakes, keeps the tunnel open, records receipt of the tx. */
  private static final class StubNode implements Closeable {

    private final ServerSocket serverSocket;
    private final AtomicBoolean received;
    // When false the relay is "degraded": it drops the connection during the handshake.
    volatile boolean healthy = true;

    StubNode(AtomicBoolean received) throws IOException {
      this.received = received;
      this.serverSocket = new ServerSocket();
      this.serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
    }

    int getPort() {
      return serverSocket.getLocalPort();
    }

    void start() {
      Thread t = new Thread(() -> {
        while (!serverSocket.isClosed()) {
          try {
            Socket client = serverSocket.accept();
            Thread worker = new Thread(() -> {
              try {
                serve(client);
              } catch (Exception ignored) {
                // connection closed on teardown
              } finally {
                try {
                  client.close();
                } catch (IOException ignored) {
                  // ignore
                }
              }
            });
            worker.setDaemon(true);
            worker.start();
          } catch (Exception e) {
            return; // server socket closed on teardown
          }
        }
      }, "stub-tron-node");
      t.setDaemon(true);
      t.start();
    }

    private void serve(Socket client) throws Exception {
      InputStream in = client.getInputStream();
      OutputStream out = client.getOutputStream();

      // Transport Hello (uncompressed); reject if the client's "from" endpoint is invalid.
      Connect.HelloMessage clientHello = Connect.HelloMessage.parseFrom(tail(readFrame(in)));
      if (!NetUtil.validNode(NetUtil.getNode(clientHello.getFrom()))) {
        return;
      }
      if (!healthy) {
        return; // degraded relay: drop the connection mid-handshake
      }
      Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
          .setFrom(Connect.HelloMessage.getDefaultInstance().getFrom())
          .setNetworkId(NETWORK_ID).setCode(0).setVersion(1)
          .setTimestamp(System.currentTimeMillis()).build();
      // rebuild with a valid endpoint
      org.tron.p2p.protos.Discover.Endpoint ep = org.tron.p2p.protos.Discover.Endpoint.newBuilder()
          .setNodeId(ByteString.copyFrom(NetUtil.getNodeId()))
          .setAddress(ByteString.copyFrom("203.0.113.7".getBytes())).setPort(18888).build();
      hello = hello.toBuilder().setFrom(ep).build();
      writeFrame(out, prepend(HELLO, hello.toByteArray()));

      // Application-level P2P_HELLO advertising a head block (compressed, post-handshake).
      Protocol.HelloMessage.BlockId block = Protocol.HelloMessage.BlockId.newBuilder()
          .setHash(ByteString.copyFrom(new byte[32])).setNumber(1L).build();
      org.tron.protos.Discover.Endpoint appFrom = org.tron.protos.Discover.Endpoint.newBuilder()
          .setNodeId(ByteString.copyFrom(NetUtil.getNodeId()))
          .setAddress(ByteString.copyFrom("203.0.113.7".getBytes())).setPort(18888).build();
      Protocol.HelloMessage appHello = Protocol.HelloMessage.newBuilder()
          .setFrom(appFrom).setVersion(NETWORK_ID).setTimestamp(System.currentTimeMillis())
          .setGenesisBlockId(block).setSolidBlockId(block).setHeadBlockId(block)
          .setNodeType(0).setLowestBlockNum(0).build();
      writeFrame(out, UpgradeController.codeSendData(1,
          prepend(MessageTypes.P2P_HELLO.asByte(), appHello.toByteArray())));

      // Read frames until the client advertises the tx (INV), then request it and read the batch.
      long deadline = System.currentTimeMillis() + 15000;
      while (System.currentTimeMillis() < deadline) {
        byte[] frame = UpgradeController.decodeReceiveData(1, readFrame(in));
        if (frame.length == 0) {
          continue;
        }
        if (frame[0] == MessageTypes.INVENTORY.asByte()) {
          InventoryMessage invMessage = new InventoryMessage(tail(frame));
          FetchInvDataMessage fetch =
              new FetchInvDataMessage(invMessage.getHashList(), InventoryType.TRX);
          writeFrame(out, UpgradeController.codeSendData(1, fetch.getSendBytes()));
        } else if (frame[0] == MessageTypes.TRXS.asByte()) {
          received.set(true);
          return;
        }
      }
    }

    @Override
    public void close() {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }

  /** Minimal Tor ControlPort stub: accepts AUTHENTICATE then records a SIGNAL NEWNYM. */
  private static final class TorControlServer implements Closeable {

    private final ServerSocket serverSocket;
    final AtomicBoolean newnymReceived = new AtomicBoolean(false);

    TorControlServer() throws IOException {
      serverSocket = new ServerSocket();
      serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
    }

    int getPort() {
      return serverSocket.getLocalPort();
    }

    void start() {
      Thread t = new Thread(() -> {
        while (!serverSocket.isClosed()) {
          try {
            Socket client = serverSocket.accept();
            Thread worker = new Thread(() -> handle(client));
            worker.setDaemon(true);
            worker.start();
          } catch (IOException e) {
            return;
          }
        }
      }, "test-tor-control");
      t.setDaemon(true);
      t.start();
    }

    private void handle(Socket client) {
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(
            client.getInputStream(), StandardCharsets.US_ASCII));
        OutputStream out = client.getOutputStream();
        String line;
        while ((line = in.readLine()) != null) {
          if (line.startsWith("AUTHENTICATE") || line.startsWith("SIGNAL NEWNYM")) {
            if (line.startsWith("SIGNAL NEWNYM")) {
              newnymReceived.set(true);
            }
            out.write("250 OK\r\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();
          }
        }
      } catch (IOException ignored) {
        // connection closed
      }
    }

    @Override
    public void close() {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }

  /** Minimal no-auth SOCKS5 forwarder: CONNECT, then blindly pipe bytes both ways. */
  private static final class Socks5Forwarder implements Closeable {

    private final ServerSocket serverSocket;

    Socks5Forwarder() throws IOException {
      serverSocket = new ServerSocket();
      serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
    }

    int getPort() {
      return serverSocket.getLocalPort();
    }

    void start() {
      Thread t = new Thread(() -> {
        while (!serverSocket.isClosed()) {
          try {
            Socket client = serverSocket.accept();
            Thread worker = new Thread(() -> handle(client));
            worker.setDaemon(true);
            worker.start();
          } catch (IOException e) {
            return;
          }
        }
      }, "test-socks5");
      t.setDaemon(true);
      t.start();
    }

    private void handle(Socket client) {
      try {
        InputStream in = client.getInputStream();
        OutputStream out = client.getOutputStream();
        readNBytes(in, 1); // version
        int nMethods = in.read();
        readNBytes(in, nMethods);
        out.write(new byte[] {0x05, 0x00});
        out.flush();

        readNBytes(in, 3); // ver, cmd, rsv
        int atyp = in.read();
        byte[] addr;
        if (atyp == 0x01) {
          addr = readNBytes(in, 4);
        } else if (atyp == 0x03) {
          addr = readNBytes(in, in.read());
        } else {
          addr = readNBytes(in, 16);
        }
        int port = ((in.read() & 0xff) << 8) | (in.read() & 0xff);
        String host = atyp == 0x03 ? new String(addr, java.nio.charset.StandardCharsets.US_ASCII)
            : java.net.InetAddress.getByAddress(addr).getHostAddress();

        Socket target = new Socket(host, port);
        out.write(new byte[] {0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
        out.flush();
        pipe(client, target);
        pipe(target, client);
      } catch (IOException e) {
        closeQuietly(client);
      }
    }

    private void pipe(Socket from, Socket to) {
      Thread t = new Thread(() -> {
        byte[] buf = new byte[4096];
        try {
          InputStream src = from.getInputStream();
          OutputStream dst = to.getOutputStream();
          int n;
          while ((n = src.read(buf)) != -1) {
            dst.write(buf, 0, n);
            dst.flush();
          }
        } catch (IOException ignored) {
          // closed
        } finally {
          closeQuietly(from);
          closeQuietly(to);
        }
      });
      t.setDaemon(true);
      t.start();
    }

    private static byte[] readNBytes(InputStream in, int n) throws IOException {
      byte[] buf = new byte[n];
      int off = 0;
      while (off < n) {
        int r = in.read(buf, off, n - off);
        if (r == -1) {
          throw new IOException("closed");
        }
        off += r;
      }
      return buf;
    }

    private static void closeQuietly(Socket socket) {
      try {
        socket.close();
      } catch (IOException ignored) {
        // ignore
      }
    }

    @Override
    public void close() {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }
}
