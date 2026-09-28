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

import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Settings;
import org.junit.Test;

/** Guards the netty HTTP/2 header-size behaviour the gRPC server relies on. */
public class NettyHttp2HeaderSecurityTest {

  /**
   * CVE-2026-50560: SETTINGS_MAX_HEADER_LIST_SIZE tells the server what the client is willing to
   * receive, so it must not shrink the server encoder's own limit. Otherwise a hostile client can
   * advertise a tiny value and make every response-header write throw, which is a Rapid-Reset-like
   * denial of service. Netty enforced the client value before 4.1.135.Final / 4.2.15.Final.
   */
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
}
