/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with java-tron.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import io.grpc.ChannelLogger;
import io.grpc.ChannelLogger.ChannelLogLevel;
import io.grpc.netty.GrpcHttp2ConnectionHandler;
import io.grpc.netty.InternalProtocolNegotiator;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Settings;
import org.junit.Test;

public class GrpcNettyMaxConcurrentStreamsLimiterTest {

  private static final ChannelLogger NOOP_LOGGER = new ChannelLogger() {
    @Override
    public void log(ChannelLogLevel level, String message) {
    }

    @Override
    public void log(ChannelLogLevel level, String messageFormat, Object... args) {
    }
  };

  @Test
  public void shouldEnforceMaxStreamsBeforeSettingsAck() throws Exception {
    Http2Connection connection = new DefaultHttp2Connection(true);
    GrpcHttp2ConnectionHandler grpcHandler = newGrpcHandler(connection);
    InternalProtocolNegotiator.ProtocolNegotiator negotiator =
        GrpcNettyMaxConcurrentStreamsLimiter.newPlaintextNegotiator(2);

    ChannelHandler negotiationHandler = negotiator.newHandler(grpcHandler);

    assertNotNull(negotiationHandler);
    assertEquals(2, connection.remote().maxActiveStreams());
    connection.remote().createStream(1, true);
    connection.remote().createStream(3, true);
    Http2Exception exception = assertThrows(
        Http2Exception.class, () -> connection.remote().createStream(5, true));
    assertEquals(Http2Error.REFUSED_STREAM, exception.error());
    negotiator.close();
  }

  @Test
  public void shouldIgnoreClientMaxHeaderListSizeOnServer() throws Exception {
    Http2Connection connection = new DefaultHttp2Connection(true);
    Http2FrameWriter frameWriter = new DefaultHttp2FrameWriter();
    Http2ConnectionEncoder encoder =
        new DefaultHttp2ConnectionEncoder(connection, frameWriter);
    long originalMaxHeaderListSize =
        encoder.configuration().headersConfiguration().maxHeaderListSize();

    encoder.remoteSettings(new Http2Settings().maxHeaderListSize(1));

    assertEquals(originalMaxHeaderListSize,
        encoder.configuration().headersConfiguration().maxHeaderListSize());
    encoder.close();
  }

  @Test
  public void shouldRejectNonPositiveStreamLimit() {
    IllegalArgumentException zeroLimitException = assertThrows(IllegalArgumentException.class,
        () -> GrpcNettyMaxConcurrentStreamsLimiter.newPlaintextNegotiator(0));
    assertEquals("maxConcurrentStreams must be positive", zeroLimitException.getMessage());
    IllegalArgumentException negativeLimitException = assertThrows(IllegalArgumentException.class,
        () -> GrpcNettyMaxConcurrentStreamsLimiter.newPlaintextNegotiator(-1));
    assertEquals("maxConcurrentStreams must be positive", negativeLimitException.getMessage());
  }

  private static GrpcHttp2ConnectionHandler newGrpcHandler(Http2Connection connection) {
    Http2FrameWriter frameWriter = new DefaultHttp2FrameWriter();
    Http2ConnectionEncoder encoder =
        new DefaultHttp2ConnectionEncoder(connection, frameWriter);
    Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(
        connection, encoder, new DefaultHttp2FrameReader());
    return new GrpcHttp2ConnectionHandler(
        null, decoder, encoder, new Http2Settings(), NOOP_LOGGER) {
    };
  }
}
