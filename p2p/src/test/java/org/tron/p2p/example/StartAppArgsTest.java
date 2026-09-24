package org.tron.p2p.example;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * StartApp's command-line parsing.
 *
 * <p>The class itself is excluded from the coverage report as a standalone entry
 * point, but these two helpers are real logic and one of them shipped a bug:
 * --trust-ips is declared as ip[,ip[...]] yet resolved the whole comma-separated
 * value as a single hostname, so with more than one address none of the listed
 * peers became trusted.
 */
public class StartAppArgsTest {

  private final StartApp app = new StartApp();

  @Test
  public void trustIpsSplitsOnComma() {
    List<InetAddress> parsed = app.parseInetAddressList("127.0.0.2,127.0.0.3");

    Assert.assertEquals(2, parsed.size());
    Assert.assertEquals("127.0.0.2", parsed.get(0).getHostAddress());
    Assert.assertEquals("127.0.0.3", parsed.get(1).getHostAddress());
  }

  @Test
  public void trustIpsAcceptsASingleAddress() {
    List<InetAddress> parsed = app.parseInetAddressList("127.0.0.2");
    Assert.assertEquals(1, parsed.size());
    Assert.assertEquals("127.0.0.2", parsed.get(0).getHostAddress());
  }

  @Test
  public void trustIpsToleratesSpacesAndEmptyEntries() {
    List<InetAddress> parsed = app.parseInetAddressList(" 127.0.0.2 , ,127.0.0.3,");
    Assert.assertEquals(2, parsed.size());
  }

  @Test
  public void trustIpsSkipsWhatItCannotResolve() {
    // An unresolvable entry is logged and dropped rather than aborting the rest.
    List<InetAddress> parsed =
        app.parseInetAddressList("127.0.0.2,no-such-host.invalid,127.0.0.3");
    Assert.assertEquals(2, parsed.size());
    Assert.assertEquals("127.0.0.2", parsed.get(0).getHostAddress());
    Assert.assertEquals("127.0.0.3", parsed.get(1).getHostAddress());
  }

  @Test
  public void seedNodesParseHostAndPort() {
    List<InetSocketAddress> parsed =
        app.parseInetSocketAddressList("127.0.0.1:18888,127.0.0.2:18889");

    Assert.assertEquals(2, parsed.size());
    Assert.assertEquals(18888, parsed.get(0).getPort());
    Assert.assertEquals("127.0.0.1", parsed.get(0).getAddress().getHostAddress());
    Assert.assertEquals(18889, parsed.get(1).getPort());
  }

  @Test
  public void seedNodesAcceptBracketedIpv6() {
    List<InetSocketAddress> parsed = app.parseInetSocketAddressList("[::1]:18888");
    Assert.assertEquals(1, parsed.size());
    Assert.assertEquals(18888, parsed.get(0).getPort());
  }
}
