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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.grpc.netty.GrpcHttp2ConnectionHandler;
import io.grpc.netty.InternalProtocolNegotiator;
import io.grpc.netty.InternalProtocolNegotiators;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.ChannelHandler;
import io.netty.util.AsciiString;

/** Enforces the advertised HTTP/2 concurrent stream limit for grpc-netty servers. */
final class GrpcNettyMaxConcurrentStreamsLimiter {

  private GrpcNettyMaxConcurrentStreamsLimiter() {
  }

  static NettyServerBuilder configurePlaintext(
      NettyServerBuilder builder, int maxConcurrentStreams) {
    checkNotNull(builder, "builder");
    checkArgument(maxConcurrentStreams > 0, "maxConcurrentStreams must be positive");
    builder.maxConcurrentCallsPerConnection(maxConcurrentStreams);
    // TODO: Remove this shim after https://github.com/grpc/grpc-java/issues/12930 is fixed.
    return builder.protocolNegotiator(newPlaintextNegotiator(maxConcurrentStreams));
  }

  static InternalProtocolNegotiator.ProtocolNegotiator newPlaintextNegotiator(
      int maxConcurrentStreams) {
    checkArgument(maxConcurrentStreams > 0, "maxConcurrentStreams must be positive");
    return new EnforcingProtocolNegotiator(
        InternalProtocolNegotiators.serverPlaintext(), maxConcurrentStreams);
  }

  private static final class EnforcingProtocolNegotiator
      implements InternalProtocolNegotiator.ProtocolNegotiator {

    private final InternalProtocolNegotiator.ProtocolNegotiator delegate;
    private final int maxConcurrentStreams;

    private EnforcingProtocolNegotiator(
        InternalProtocolNegotiator.ProtocolNegotiator delegate, int maxConcurrentStreams) {
      this.delegate = checkNotNull(delegate, "delegate");
      this.maxConcurrentStreams = maxConcurrentStreams;
    }

    @Override
    public AsciiString scheme() {
      return delegate.scheme();
    }

    @Override
    public ChannelHandler newHandler(GrpcHttp2ConnectionHandler grpcHandler) {
      // grpc-java builds the connection directly, bypassing Netty's builder-side enforcement.
      grpcHandler.connection().remote().maxActiveStreams(maxConcurrentStreams);
      return delegate.newHandler(grpcHandler);
    }

    @Override
    public void close() {
      delegate.close();
    }
  }
}
