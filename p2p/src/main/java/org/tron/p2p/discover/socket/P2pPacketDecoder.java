package org.tron.p2p.discover.socket;

import com.google.common.util.concurrent.RateLimiter;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.event.Level;
import org.tron.p2p.base.Constant;
import org.tron.p2p.discover.message.Message;
import org.tron.p2p.exception.P2pException;

@Slf4j(topic = "net")
public class P2pPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

  private static final int REPORT_INTERVAL_SECONDS = 60;
  private static final int MAX_LOG_DATA_BYTES = 128;
  private static final String SUMMARY =
      "Suppressed UDP discovery logs: reason={}, suppressed={}, total={}";

  // Only the channel's event loop updates the counters and reports summaries.
  private final EnumMap<DropReason, DropCounter> drops = new EnumMap<>(DropReason.class);
  private ScheduledFuture<?> summaryTask;

  public P2pPacketDecoder() {
    for (DropReason reason : DropReason.values()) {
      drops.put(reason, new DropCounter());
    }
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    summaryTask = ctx.executor().scheduleWithFixedDelay(this::flushSummaries,
        REPORT_INTERVAL_SECONDS, REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    if (summaryTask != null) {
      summaryTask.cancel(false);
      summaryTask = null;
    }
    flushSummaries();
  }

  @Override
  public void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out)
      throws Exception {
    ByteBuf buf = packet.content();
    int length = buf.readableBytes();
    if (length <= 1 || length >= Constant.MAX_DISCOVERY_PACKET_SIZE) {
      if (shouldLog(DropReason.INVALID_LENGTH)) {
        logger.warn("UDP rcv bad packet, from {} length = {}", packet.sender(), length);
      }
      return;
    }
    byte[] encoded = new byte[length];
    buf.readBytes(encoded);
    try {
      UdpEvent event = new UdpEvent(Message.parse(encoded), packet.sender());
      out.add(event);
    } catch (P2pException pe) {
      if (pe.getType() == P2pException.TypeEnum.BAD_MESSAGE) {
        if (shouldLog(DropReason.INVALID_MESSAGE)) {
          logger.error("Message validation failed, type {}, len {}, address {}",
              encoded[0], length, packet.sender());
        }
      } else if (shouldLog(DropReason.PARSE_FAILURE)) {
        logger.info("Parse msg failed, type {}, len {}, address {}",
            encoded[0], length, packet.sender());
      }
    } catch (InvalidProtocolBufferException e) {
      if (shouldLog(DropReason.INVALID_PROTOBUF)) {
        logger.warn("An exception occurred while parsing the message, type {}, len {}, address {}, "
                + "data {}, cause: {}", encoded[0], length, packet.sender(), dataPreview(encoded),
            e.getClass().getSimpleName());
      }
    } catch (Exception e) {
      if (shouldLog(DropReason.UNEXPECTED_ERROR)) {
        logger.error("An exception occurred while parsing the message, type {}, len {}, "
            + "address {}, data {}", encoded[0], length, packet.sender(), dataPreview(encoded));
      }
    }
  }

  static String dataPreview(byte[] encoded) {
    int length = Integer.min(encoded.length, MAX_LOG_DATA_BYTES);
    String hex = Hex.toHexString(encoded, 0, length);
    return length < encoded.length ? hex + "..." : hex;
  }

  private boolean shouldLog(DropReason reason) {
    DropCounter counter = drops.get(reason);
    if (!reason.isEnabled() || !counter.trySample()) {
      return false;
    }
    reportSuppressed(reason, counter);
    return true;
  }

  private void flushSummaries() {
    drops.forEach(P2pPacketDecoder::reportSuppressed);
  }

  private static void reportSuppressed(DropReason reason, DropCounter counter) {
    if (counter.suppressed == 0 || !reason.isEnabled()) {
      return;
    }
    switch (reason.level) {
      case WARN:
        logger.warn(SUMMARY, reason, counter.suppressed, counter.totalWhileEnabled);
        break;
      case INFO:
        logger.info(SUMMARY, reason, counter.suppressed, counter.totalWhileEnabled);
        break;
      case ERROR:
        logger.error(SUMMARY, reason, counter.suppressed, counter.totalWhileEnabled);
        break;
      default:
        throw new AssertionError(reason.level);
    }
    counter.suppressed = 0;
  }

  private enum DropReason {
    INVALID_LENGTH(Level.WARN),
    INVALID_MESSAGE(Level.ERROR),
    PARSE_FAILURE(Level.INFO),
    INVALID_PROTOBUF(Level.WARN),
    UNEXPECTED_ERROR(Level.ERROR);

    final Level level;

    DropReason(Level level) {
      this.level = level;
    }

    boolean isEnabled() {
      switch (level) {
        case WARN:
          return logger.isWarnEnabled();
        case INFO:
          return logger.isInfoEnabled();
        case ERROR:
          return logger.isErrorEnabled();
        default:
          throw new AssertionError(level);
      }
    }
  }

  static final class DropCounter {
    private final RateLimiter limiter = RateLimiter.create(1.0 / REPORT_INTERVAL_SECONDS);
    long totalWhileEnabled;
    long suppressed;

    boolean trySample() {
      totalWhileEnabled++;
      if (limiter.tryAcquire()) {
        return true;
      }
      suppressed++;
      return false;
    }
  }
}
