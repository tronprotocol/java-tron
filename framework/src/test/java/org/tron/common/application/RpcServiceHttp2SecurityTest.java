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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.math.StrictMathWrapper.min;

import com.google.protobuf.Empty;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersEncoder;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;

public class RpcServiceHttp2SecurityTest {

  private static final int HEADERS_FRAME_TYPE = 0x1;
  private static final int RST_STREAM_FRAME_TYPE = 0x3;
  private static final int SETTINGS_FRAME_TYPE = 0x4;
  private static final int GO_AWAY_FRAME_TYPE = 0x7;
  private static final int SETTINGS_MAX_CONCURRENT_STREAMS = 0x3;
  private static final byte[] CLIENT_PREFACE =
      "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] EMPTY_SETTINGS_FRAME =
      new byte[]{0, 0, 0, 4, 0, 0, 0, 0, 0};
  private static final String SERVICE_NAME = "test.HoldService";
  private static final String METHOD_NAME = "Hold";
  private static final String METHOD_PATH = "/" + SERVICE_NAME + "/" + METHOD_NAME;

  private CommonParameter parameter;
  private int previousRpcThreadNum;
  private int previousMaxConcurrentCalls;
  private int previousFlowControlWindow;
  private long previousMaxConnectionIdle;
  private long previousMaxConnectionAge;
  private int previousMaxMessageSize;
  private int previousMaxHeaderListSize;
  private int previousMaxRstStream;
  private int previousSecondsPerWindow;
  private boolean previousReflectionServiceEnable;

  @Before
  public void setUp() {
    parameter = Args.getInstance();
    previousRpcThreadNum = parameter.getRpcThreadNum();
    previousMaxConcurrentCalls = parameter.getMaxConcurrentCallsPerConnection();
    previousFlowControlWindow = parameter.getFlowControlWindow();
    previousMaxConnectionIdle = parameter.getMaxConnectionIdleInMillis();
    previousMaxConnectionAge = parameter.getMaxConnectionAgeInMillis();
    previousMaxMessageSize = parameter.getMaxMessageSize();
    previousMaxHeaderListSize = parameter.getMaxHeaderListSize();
    previousMaxRstStream = parameter.getRpcMaxRstStream();
    previousSecondsPerWindow = parameter.getRpcSecondsPerWindow();
    previousReflectionServiceEnable = parameter.isRpcReflectionServiceEnable();

    parameter.setRpcThreadNum(0);
    parameter.setMaxConcurrentCallsPerConnection(2);
    parameter.setFlowControlWindow(NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW);
    parameter.setMaxConnectionIdleInMillis(60_000);
    parameter.setMaxConnectionAgeInMillis(Long.MAX_VALUE);
    parameter.setMaxMessageSize(4 * 1024 * 1024);
    parameter.setMaxHeaderListSize(8 * 1024);
    parameter.setRpcMaxRstStream(0);
    parameter.setRpcSecondsPerWindow(0);
    parameter.setRpcReflectionServiceEnable(false);
  }

  @After
  public void tearDown() {
    parameter.setRpcThreadNum(previousRpcThreadNum);
    parameter.setMaxConcurrentCallsPerConnection(previousMaxConcurrentCalls);
    parameter.setFlowControlWindow(previousFlowControlWindow);
    parameter.setMaxConnectionIdleInMillis(previousMaxConnectionIdle);
    parameter.setMaxConnectionAgeInMillis(previousMaxConnectionAge);
    parameter.setMaxMessageSize(previousMaxMessageSize);
    parameter.setMaxHeaderListSize(previousMaxHeaderListSize);
    parameter.setRpcMaxRstStream(previousMaxRstStream);
    parameter.setRpcSecondsPerWindow(previousSecondsPerWindow);
    parameter.setRpcReflectionServiceEnable(previousReflectionServiceEnable);
  }

  @Test
  public void shouldRejectExcessStreamsBeforeClientAcknowledgesSettings() throws Exception {
    TestRpcService rpcService = new TestRpcService();
    Server server = rpcService.newServerBuilder()
        .addService(newHoldService())
        .build()
        .start();

    try (Socket socket = new Socket("127.0.0.1", server.getPort())) {
      socket.setSoTimeout(5_000);
      OutputStream output = socket.getOutputStream();
      output.write(CLIENT_PREFACE);
      output.write(EMPTY_SETTINGS_FRAME);
      output.write(newHeadersFrame(1));
      output.write(newHeadersFrame(3));
      output.write(newHeadersFrame(5));
      output.flush();

      assertSettingsAndRefusedStream(socket.getInputStream(), 2, 5);
    } finally {
      server.shutdownNow();
      assertTrue(server.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static ServerServiceDefinition newHoldService() {
    MethodDescriptor<Empty, Empty> method =
        MethodDescriptor.<Empty, Empty>newBuilder()
            .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(
                SERVICE_NAME, METHOD_NAME))
            .setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
            .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
            .build();

    return ServerServiceDefinition.builder(SERVICE_NAME)
        .addMethod(method, new ServerCallHandler<Empty, Empty>() {
          @Override
          public ServerCall.Listener<Empty> startCall(
              ServerCall<Empty, Empty> call, Metadata headers) {
            return new ServerCall.Listener<Empty>() {
            };
          }
        })
        .build();
  }

  private static byte[] newHeadersFrame(int streamId) throws Exception {
    DefaultHttp2HeadersEncoder encoder = new DefaultHttp2HeadersEncoder();
    ByteBuf headerBlock = Unpooled.buffer();
    ByteBuf frame = Unpooled.buffer();
    try {
      Http2Headers headers = new DefaultHttp2Headers()
          .method("POST")
          .scheme("http")
          .authority("localhost")
          .path(METHOD_PATH)
          .set("content-type", "application/grpc")
          .set("te", "trailers");
      encoder.encodeHeaders(streamId, headers, headerBlock);

      frame.writeMedium(headerBlock.readableBytes());
      frame.writeByte(HEADERS_FRAME_TYPE);
      frame.writeByte(0x4);
      frame.writeInt(streamId);
      frame.writeBytes(headerBlock);
      return ByteBufUtil.getBytes(frame);
    } finally {
      frame.release();
      headerBlock.release();
      encoder.close();
    }
  }

  private static void assertSettingsAndRefusedStream(
      InputStream input, long expectedMaxConcurrentStreams, int expectedRefusedStreamId)
      throws IOException {
    boolean advertisedLimitFound = false;
    for (int i = 0; i < 20; i++) {
      Http2Frame frame = readFrame(input);
      if (frame.type == SETTINGS_FRAME_TYPE && frame.streamId == 0) {
        advertisedLimitFound |= hasSetting(
            frame.payload, SETTINGS_MAX_CONCURRENT_STREAMS, expectedMaxConcurrentStreams);
      }
      if (frame.type == RST_STREAM_FRAME_TYPE && frame.streamId == expectedRefusedStreamId) {
        assertTrue("Server did not advertise the enforced concurrent-stream limit",
            advertisedLimitFound);
        assertEquals(4, frame.payload.length);
        long errorCode = ByteBuffer.wrap(frame.payload).getInt() & 0xffff_ffffL;
        assertEquals(Http2Error.REFUSED_STREAM.code(), errorCode);
        return;
      }
      if (frame.type == GO_AWAY_FRAME_TYPE) {
        fail("Server closed the connection instead of refusing only the excess stream");
      }
    }
    fail("No REFUSED_STREAM response for stream " + expectedRefusedStreamId);
  }

  private static boolean hasSetting(byte[] payload, int expectedId, long expectedValue) {
    assertEquals("Invalid HTTP/2 SETTINGS payload length", 0, payload.length % 6);
    ByteBuffer settings = ByteBuffer.wrap(payload);
    while (settings.remaining() >= 6) {
      int id = settings.getShort() & 0xffff;
      long value = settings.getInt() & 0xffff_ffffL;
      if (id == expectedId) {
        assertEquals(expectedValue, value);
        return true;
      }
    }
    return false;
  }

  private static Http2Frame readFrame(InputStream input) throws IOException {
    byte[] header = readFully(input, 9);
    int payloadLength =
        ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8) | (header[2] & 0xff);
    int type = header[3] & 0xff;
    int streamId = ByteBuffer.wrap(header, 5, 4).getInt() & 0x7fff_ffff;
    return new Http2Frame(type, streamId, readFully(input, payloadLength));
  }

  private static byte[] readFully(InputStream input, int length) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream(length);
    byte[] buffer = new byte[min(length, 1024)];
    while (output.size() < length) {
      int read = input.read(buffer, 0, min(buffer.length, length - output.size()));
      if (read < 0) {
        throw new EOFException("Unexpected end of HTTP/2 frame");
      }
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  private static final class Http2Frame {

    private final int type;
    private final int streamId;
    private final byte[] payload;

    private Http2Frame(int type, int streamId, byte[] payload) {
      this.type = type;
      this.streamId = streamId;
      this.payload = payload;
    }
  }

  private static final class TestRpcService extends RpcService {

    private TestRpcService() {
      port = 0;
    }

    private NettyServerBuilder newServerBuilder() {
      return initServerBuilder();
    }

    @Override
    protected void addService(NettyServerBuilder serverBuilder) {
    }
  }
}
