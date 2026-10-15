package org.tron.p2p.example;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.junit.Assert;
import org.junit.Test;

/**
 * StartApp's command-line parsing.
 *
 * <p>The class itself is excluded from the coverage report as a standalone entry
 * point, but its parsing helpers are real logic and one of them shipped a bug:
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

  @Test
  public void parseBlockedIpsFromCli() throws Exception {
    Method parseCli = StartApp.class.getDeclaredMethod("parseCli", String[].class);
    parseCli.setAccessible(true);
    for (String option : new String[]{"--blocked-ips", "-b"}) {
      CommandLine cli = (CommandLine) parseCli.invoke(app,
          (Object) new String[]{option, "192.0.2.1, 2001:db8::1 ,192.0.2.1"});

      Set<InetAddress> blockedIps = app.parseInetAddressSet(cli.getOptionValue("b"));

      Assert.assertEquals(2, blockedIps.size());
      Assert.assertTrue(blockedIps.contains(InetAddress.getByName("192.0.2.1")));
      Assert.assertTrue(blockedIps.contains(InetAddress.getByName("2001:db8::1")));
    }
  }

  @Test
  public void blockedIpsAcceptsIpv4AnyAddress() throws Exception {
    Set<InetAddress> blockedIps = app.parseInetAddressSet("0.0.0.0");

    Assert.assertEquals(1, blockedIps.size());
    Assert.assertTrue(blockedIps.contains(InetAddress.getByName("0.0.0.0")));
  }

  @Test
  public void rejectScopedAndAmbiguousBlockedIps() {
    for (String ip : new String[] {"fe80::1%eth0", "fe80::1%1", "192.0.02.1"}) {
      try {
        app.parseInetAddressSet(ip);
        Assert.fail("Expected invalid blocked IP: " + ip);
      } catch (IllegalArgumentException expected) {
        Assert.assertTrue(expected.getMessage().startsWith("Invalid blocked IP: "));
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectHostNameAsBlockedIp() {
    app.parseInetAddressSet("malicious.example.com");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectEmptyBlockedIpEntry() {
    app.parseInetAddressSet("192.0.2.1,,2001:db8::1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectTrailingEmptyBlockedIpEntry() {
    app.parseInetAddressSet("192.0.2.1,");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectNullBlockedIps() {
    app.parseInetAddressSet(null);
  }
}
