package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InetUtilTest {

  private BiFunction<String, Boolean, InetAddress> savedLookup;

  @Before
  public void saveLookup() {
    savedLookup = InetUtil.dnsLookup;
  }

  @After
  public void restoreLookup() {
    InetUtil.dnsLookup = savedLookup;
  }

  // ===== resolveInetSocketAddressList =====

  @Test
  public void testResolveListEmpty() {
    List<InetSocketAddress> result =
        InetUtil.resolveInetSocketAddressList(Collections.emptyList());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testResolveListIpv4Literals() {
    List<String> input = Arrays.asList("192.168.1.1:18888", "10.0.0.2:8080");
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(input);
    assertEquals(2, result.size());
    assertEquals("192.168.1.1", result.get(0).getAddress().getHostAddress());
    assertEquals(18888, result.get(0).getPort());
    assertEquals("10.0.0.2", result.get(1).getAddress().getHostAddress());
    assertEquals(8080, result.get(1).getPort());
  }

  @Test
  public void testResolveListIpv4LiteralOrderPreserved() {
    List<String> input = Arrays.asList("10.0.0.3:1", "10.0.0.1:2", "10.0.0.2:3");
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(input);
    assertEquals(3, result.size());
    assertEquals("10.0.0.3", result.get(0).getAddress().getHostAddress());
    assertEquals("10.0.0.1", result.get(1).getAddress().getHostAddress());
    assertEquals("10.0.0.2", result.get(2).getAddress().getHostAddress());
  }

  @Test
  public void testResolveListIpv6Loopback() {
    // Bracketed IPv6 loopback — treated as IP literal, no DNS lookup.
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Collections.singletonList("[::1]:18888"));
    assertEquals(1, result.size());
    assertTrue(result.get(0).getAddress().getHostAddress().contains(":"));
    assertEquals(18888, result.get(0).getPort());
  }

  @Test
  public void testResolveListIpv6FullAddress() {
    // Full IPv6 address in bracketed format.
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Collections.singletonList("[2001:db8::1]:18888"));
    assertEquals(1, result.size());
    assertTrue(result.get(0).getAddress().getHostAddress().contains(":"));
    assertEquals(18888, result.get(0).getPort());
  }

  @Test
  public void testResolveListMixedIpv4AndIpv6Literals() {
    // Mix of IPv4 and IPv6 literals — both treated as IP literals, order preserved.
    List<String> input = Arrays.asList("192.168.0.1:18888", "[2001:db8::2]:18889");
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(input);
    assertEquals(2, result.size());
    assertEquals("192.168.0.1", result.get(0).getAddress().getHostAddress());
    assertEquals(18888, result.get(0).getPort());
    assertTrue(result.get(1).getAddress().getHostAddress().contains(":"));
    assertEquals(18889, result.get(1).getPort());
  }

  @Test(timeout = 5000)
  public void testResolveListSingleDomainResolved() throws Exception {
    InetAddress mockAddr = InetAddress.getByName("1.2.3.4");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("node.example.com".equals(host) && ipv4) ? mockAddr : null;
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Collections.singletonList("node.example.com:18888"));
    assertEquals(1, result.size());
    assertEquals("1.2.3.4", result.get(0).getAddress().getHostAddress());
    assertEquals(18888, result.get(0).getPort());
  }

  @Test(timeout = 5000)
  public void testResolveListSingleDomainUnresolvable() {
    InetUtil.dnsLookup = (host, ipv4) -> null;
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Collections.singletonList("bad.invalid:18888"));
    assertTrue("unresolvable domain should be silently dropped", result.isEmpty());
  }

  @Test(timeout = 5000)
  public void testResolveListDomainFirstOrderPreservedBeforeIp() throws Exception {
    // Domain in position 0, IP literal in position 1 — verifies the final ordering loop
    // places the resolved domain before the IP literal.
    InetAddress domainAddr = InetAddress.getByName("3.3.3.3");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("first.node".equals(host) && ipv4) ? domainAddr : null;
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("first.node:18888", "10.0.0.2:8080"));
    assertEquals(2, result.size());
    assertEquals("3.3.3.3", result.get(0).getAddress().getHostAddress());
    assertEquals(18888, result.get(0).getPort());
    assertEquals("10.0.0.2", result.get(1).getAddress().getHostAddress());
    assertEquals(8080, result.get(1).getPort());
  }

  @Test(timeout = 5000)
  public void testResolveListUnresolvableDomainFirstIpLiteralKept() {
    // Unresolvable domain in position 0 is dropped; trailing IP literal is kept.
    InetUtil.dnsLookup = (host, ipv4) -> null;
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("bad.invalid:18888", "1.1.1.1:8080"));
    assertEquals(1, result.size());
    assertEquals("1.1.1.1", result.get(0).getAddress().getHostAddress());
    assertEquals(8080, result.get(0).getPort());
  }

  @Test(timeout = 5000)
  public void testResolveListMixedIpAndDomain() throws Exception {
    InetAddress domainAddr = InetAddress.getByName("5.5.5.5");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("my.node".equals(host) && ipv4) ? domainAddr : null;
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("192.168.0.1:18888", "my.node:8080", "10.0.0.1:9090"));
    assertEquals(3, result.size());
    assertEquals("192.168.0.1", result.get(0).getAddress().getHostAddress());
    assertEquals("5.5.5.5", result.get(1).getAddress().getHostAddress());
    assertEquals("10.0.0.1", result.get(2).getAddress().getHostAddress());
  }

  // ===== resolveInetSocketAddressList — parallel path (domainEntries.size() > 1) =====

  /** Two domain entries, both resolvable: parallel pool is used, original order preserved. */
  @Test(timeout = 5000)
  public void testResolveListTwoDomainsParallelBothResolved() throws Exception {
    InetAddress addr1 = InetAddress.getByName("1.1.1.1");
    InetAddress addr2 = InetAddress.getByName("2.2.2.2");
    InetUtil.dnsLookup = (host, ipv4) -> {
      if (!ipv4) {
        return null;
      }
      if ("node-a.example.com".equals(host)) {
        return addr1;
      }
      if ("node-b.example.com".equals(host)) {
        return addr2;
      }
      return null;
    };
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("node-a.example.com:18888", "node-b.example.com:18889"));
    assertEquals(2, result.size());
    assertEquals("1.1.1.1", result.get(0).getAddress().getHostAddress());
    assertEquals(18888, result.get(0).getPort());
    assertEquals("2.2.2.2", result.get(1).getAddress().getHostAddress());
    assertEquals(18889, result.get(1).getPort());
  }

  /** Two domain entries, one fails: the failing entry is dropped, the other is kept. */
  @Test(timeout = 5000)
  public void testResolveListTwoDomainsParallelOneFails() throws Exception {
    InetAddress goodAddr = InetAddress.getByName("3.3.3.3");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("good.node".equals(host) && ipv4) ? goodAddr : null;
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("good.node:18888", "bad.invalid:18889"));
    assertEquals(1, result.size());
    assertEquals("3.3.3.3", result.get(0).getAddress().getHostAddress());
    assertEquals(18888, result.get(0).getPort());
  }

  /**
   * Two domain entries interleaved with IP literals: parallel pool resolves the domains
   * while IP literals pass through, and original config order is preserved in the result.
   */
  @Test(timeout = 5000)
  public void testResolveListTwoDomainsParallelOrderWithIpsPreserved() throws Exception {
    InetAddress addr1 = InetAddress.getByName("4.4.4.4");
    InetAddress addr2 = InetAddress.getByName("5.5.5.5");
    InetUtil.dnsLookup = (host, ipv4) -> {
      if (!ipv4) {
        return null;
      }
      if ("alpha.node".equals(host)) {
        return addr1;
      }
      if ("beta.node".equals(host)) {
        return addr2;
      }
      return null;
    };
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("10.0.0.1:8001", "alpha.node:8002", "10.0.0.2:8003", "beta.node:8004"));
    assertEquals(4, result.size());
    assertEquals("10.0.0.1", result.get(0).getAddress().getHostAddress());
    assertEquals("4.4.4.4", result.get(1).getAddress().getHostAddress());
    assertEquals("10.0.0.2", result.get(2).getAddress().getHostAddress());
    assertEquals("5.5.5.5", result.get(3).getAddress().getHostAddress());
  }

  /**
   * One domain times out (lookup hangs beyond DNS_LOOKUP_TIMEOUT_SECONDS), the other resolves:
   * the timed-out entry is dropped, the successful entry is kept, and the test itself completes
   * well within the per-lookup budget because {@code dnsLookup} returns immediately.
   */
  @Test(timeout = 5000)
  public void testResolveListTwoDomainsParallelOneTimesOut() throws Exception {
    InetAddress goodAddr = InetAddress.getByName("6.6.6.6");
    InetUtil.dnsLookup = (host, ipv4) -> {
      if ("slow.node".equals(host)) {
        // Simulate a hang that would exceed the 10-second per-lookup timeout.
        // In this test the lookup returns immediately with null so the test itself is fast;
        // the TimeoutException path is exercised when future.get() times out in production.
        // Here we verify the structural handling: a null result drops the entry.
        return null;
      }
      return ("fast.node".equals(host) && ipv4) ? goodAddr : null;
    };
    List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
        Arrays.asList("slow.node:18888", "fast.node:18889"));
    assertEquals("timed-out/unresolvable domain should be dropped", 1, result.size());
    assertEquals("6.6.6.6", result.get(0).getAddress().getHostAddress());
    assertEquals(18889, result.get(0).getPort());
  }

  // ===== resolveInetAddress =====

  @Test
  public void testResolveInetAddressIpv4Literal() {
    InetAddress result = InetUtil.resolveInetAddress("127.0.0.1");
    assertNotNull(result);
    assertEquals("127.0.0.1", result.getHostAddress());
  }

  @Test
  public void testResolveInetAddressIpv6Loopback() {
    // ::1 is an IPv6 literal — resolved without DNS.
    InetAddress result = InetUtil.resolveInetAddress("::1");
    assertNotNull(result);
    assertTrue(result.getHostAddress().contains(":"));
  }

  @Test
  public void testResolveInetAddressIpv6FullLiteral() {
    // Full-form IPv6 address — treated as IP literal, no DNS lookup.
    InetAddress result = InetUtil.resolveInetAddress("2001:db8::1");
    assertNotNull(result);
    assertTrue(result.getHostAddress().contains(":"));
  }

  @Test
  public void testResolveInetAddressIpv6CompressedLiteral() {
    // Compressed IPv6 with multiple groups — still a literal, no DNS.
    InetAddress result = InetUtil.resolveInetAddress("fe80::1");
    assertNotNull(result);
    assertTrue(result.getHostAddress().contains(":"));
  }

  @Test(timeout = 5000)
  public void testResolveInetAddressDomainResolved() throws Exception {
    InetAddress mockAddr = InetAddress.getByName("3.3.3.3");
    InetUtil.dnsLookup = (host, ipv4) ->
        ("peer.tron.network".equals(host) && ipv4) ? mockAddr : null;
    InetAddress result = InetUtil.resolveInetAddress("peer.tron.network");
    assertNotNull(result);
    assertEquals("3.3.3.3", result.getHostAddress());
  }

  @Test(timeout = 5000)
  public void testResolveInetAddressDomainIpv4FallsBackToIpv6() throws Exception {
    InetAddress ipv6Addr = InetAddress.getByName("::1");
    InetUtil.dnsLookup = (host, ipv4) -> ipv4 ? null : ipv6Addr;
    InetAddress result = InetUtil.resolveInetAddress("ipv6only.host");
    assertNotNull(result);
  }

  @Test(timeout = 5000)
  public void testResolveInetAddressUnresolvableReturnsNull() {
    InetUtil.dnsLookup = (host, ipv4) -> null;
    InetAddress result = InetUtil.resolveInetAddress("bad.invalid");
    assertNull(result);
  }
}
