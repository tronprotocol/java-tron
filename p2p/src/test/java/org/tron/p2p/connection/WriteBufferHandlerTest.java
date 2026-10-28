package org.tron.p2p.connection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class WriteBufferHandlerTest {

  private final List<TestPeer> peers = new ArrayList<>();

  @After
  public void tearDown() {
    for (TestPeer peer : peers) {
      peer.socket.finishAndReleaseAll();
    }
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @Test
  public void peerLimitIncludesWritesAlreadyHeldByNetty() throws Exception {
    WriteBufferHandler.Budget budget = new WriteBufferHandler.Budget(50, 200);
    TestPeer peer = peer(budget);
    ByteBuf first = Unpooled.buffer(30).writeZero(30);
    ByteBuf second = Unpooled.buffer(30).writeZero(30);
    ChannelFuture firstWrite = peer.send(first);
    Assert.assertFalse(firstWrite.isDone());

    ChannelFuture secondWrite = peer.send(second);

    Assert.assertFalse(peer.socket.isOpen());
    Assert.assertTrue(firstWrite.isDone());
    Assert.assertFalse(firstWrite.isSuccess());
    Assert.assertTrue(secondWrite.isDone());
    Assert.assertFalse(secondWrite.isSuccess());
    Assert.assertEquals(0, first.refCnt());
    Assert.assertEquals(0, second.refCnt());
  }

  @Test
  public void globalLimitEvictsLargestPeerInsteadOfLatestSender() throws Exception {
    WriteBufferHandler.Budget budget = new WriteBufferHandler.Budget(100, 80);
    TestPeer largest = peer(budget);
    TestPeer middle = peer(budget);
    TestPeer latest = peer(budget);
    ByteBuf largestData = Unpooled.buffer(50).writeZero(50);
    ChannelFuture pending = largest.send(largestData);
    middle.send(20);
    latest.send(10);

    Assert.assertFalse(largest.socket.isOpen());
    Assert.assertTrue(middle.socket.isOpen());
    Assert.assertTrue(latest.socket.isOpen());
    Assert.assertTrue(pending.isDone());
    Assert.assertFalse(pending.isSuccess());
    Assert.assertEquals(0, largestData.refCnt());

    // The failed write's 51 bytes must no longer count against the shared budget.
    latest.send(30);
    Assert.assertTrue(middle.socket.isOpen());
    Assert.assertTrue(latest.socket.isOpen());
  }

  @Test
  public void completedWritesReturnBothPeerAndGlobalBudget() throws Exception {
    WriteBufferHandler.Budget budget = new WriteBufferHandler.Budget(50, 80);
    TestPeer first = peer(budget);
    TestPeer second = peer(budget);
    first.socket.writable = true;
    second.socket.writable = true;
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(first.send(30).isSuccess());
      Assert.assertTrue(second.send(30).isSuccess());
      first.socket.releaseOutbound();
      second.socket.releaseOutbound();
    }
    Assert.assertTrue(first.socket.isOpen());
    Assert.assertTrue(second.socket.isOpen());
  }

  @Test
  public void lengthPrefixCountsTowardTheLimit() throws Exception {
    TestPeer peer = peer(new WriteBufferHandler.Budget(50, 200));
    // A 50-byte payload needs an additional one-byte frame prefix.
    ChannelFuture result = peer.send(50);
    Assert.assertFalse(peer.socket.isOpen());
    Assert.assertTrue(result.isDone());
    Assert.assertFalse(result.isSuccess());
  }

  @Test
  public void onlyOneGlobalEvictionWaitsForItsWritesToBeReleased() throws Exception {
    WriteBufferHandler.Budget budget = new WriteBufferHandler.Budget(100, 80);
    TestPeer largest = peer(budget);
    TestPeer second = peer(budget);
    TestPeer third = peer(budget);
    DelayedClose close = new DelayedClose();
    largest.socket.pipeline().addFirst(close);
    try {
      largest.send(50);
      second.send(20);
      third.send(10);
      third.send(10);

      Assert.assertEquals(1, close.calls);
      Assert.assertTrue(second.socket.isOpen());
      Assert.assertTrue(third.socket.isOpen());
      Assert.assertTrue(largest.socket.isOpen());
      close.complete();
      Assert.assertFalse(largest.socket.isOpen());
      Assert.assertTrue(second.socket.isOpen());
      Assert.assertTrue(third.socket.isOpen());
    } finally {
      if (largest.socket.pipeline().context(close) != null) {
        largest.socket.pipeline().remove(close);
      }
    }
  }

  @Test
  public void rechecksGlobalLimitAfterEvictionCompletes() throws Exception {
    WriteBufferHandler.Budget budget = new WriteBufferHandler.Budget(200, 80);
    TestPeer first = peer(budget);
    TestPeer second = peer(budget);
    DelayedClose close = new DelayedClose();
    first.socket.pipeline().addFirst(close);
    try {
      first.send(50);
      second.send(30);
      second.send(60);
      Assert.assertEquals(1, close.calls);
      Assert.assertTrue(second.socket.isOpen());

      close.complete();
      Assert.assertFalse(first.socket.isOpen());
      Assert.assertFalse(second.socket.isOpen());
    } finally {
      if (first.socket.pipeline().context(close) != null) {
        first.socket.pipeline().remove(close);
      }
    }
  }

  @Test
  public void accountsForWritesBeforeTheEventLoopProcessesThem() throws Exception {
    DefaultEventLoop loop = new DefaultEventLoop();
    LocalChannel socket = new LocalChannel();
    Channel channel = new Channel();
    WriteBufferHandler handler =
        new WriteBufferHandler(channel, new WriteBufferHandler.Budget(50, 200));
    socket.pipeline().addLast(handler);
    socket.pipeline().addLast(new ChannelInboundHandlerAdapter());
    CountDownLatch blocked = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    ByteBuf first = Unpooled.buffer(30).writeZero(30);
    ByteBuf second = Unpooled.buffer(30).writeZero(30);
    try {
      loop.register(socket).sync();
      ChannelHandlerContext ctx = socket.pipeline().lastContext();
      TestPeer.set(channel, "ctx", ctx);
      InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18888);
      TestPeer.set(channel, "inetSocketAddress", address);
      TestPeer.set(channel, "inetAddress", address.getAddress());
      loop.execute(() -> {
        blocked.countDown();
        try {
          resume.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
      Assert.assertTrue(blocked.await(5, TimeUnit.SECONDS));
      ChannelFuture firstWrite = handler.writeAndFlush(ctx, first);
      Assert.assertFalse(firstWrite.isDone());
      ChannelFuture secondWrite = handler.writeAndFlush(ctx, second);

      Assert.assertTrue("the limit must apply before queued write tasks execute",
          channel.isDisconnect());
      Assert.assertEquals(0, second.refCnt());
      resume.countDown();
      Assert.assertTrue(firstWrite.await(5, TimeUnit.SECONDS));
      Assert.assertTrue(secondWrite.await(5, TimeUnit.SECONDS));
      Assert.assertEquals(0, first.refCnt());
    } finally {
      resume.countDown();
      socket.close().syncUninterruptibly();
      loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
      if (first.refCnt() > 0) {
        first.release();
      }
      if (second.refCnt() > 0) {
        second.release();
      }
    }
  }

  private TestPeer peer(WriteBufferHandler.Budget budget) throws Exception {
    TestPeer peer = new TestPeer(budget);
    peers.add(peer);
    return peer;
  }

  private static final class TestPeer {

    private final SlowSocket socket = new SlowSocket();
    private final WriteBufferHandler handler;

    private TestPeer(WriteBufferHandler.Budget budget) throws Exception {
      Channel channel = new Channel();
      handler = new WriteBufferHandler(channel, budget);
      socket.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
      socket.pipeline().addLast(handler);
      socket.pipeline().addLast(new ChannelInboundHandlerAdapter());
      set(channel, "ctx", socket.pipeline().lastContext());
      InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18888);
      set(channel, "inetSocketAddress", address);
      set(channel, "inetAddress", address.getAddress());
    }

    private ChannelFuture send(int length) {
      return send(Unpooled.buffer(length).writeZero(length));
    }

    private ChannelFuture send(ByteBuf data) {
      return handler.writeAndFlush(socket.pipeline().lastContext(), data);
    }

    private static void set(Channel channel, String name, Object value) throws Exception {
      Field field = Channel.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(channel, value);
    }
  }

  private static final class SlowSocket extends EmbeddedChannel {

    private boolean writable;

    @Override
    protected void doWrite(ChannelOutboundBuffer buffer) throws Exception {
      if (writable) {
        super.doWrite(buffer);
      }
    }
  }

  private static final class DelayedClose extends ChannelOutboundHandlerAdapter {

    private ChannelHandlerContext context;
    private ChannelPromise promise;
    private int calls;

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
      calls++;
      context = ctx;
      this.promise = promise;
    }

    private void complete() {
      context.close(promise);
    }
  }
}
