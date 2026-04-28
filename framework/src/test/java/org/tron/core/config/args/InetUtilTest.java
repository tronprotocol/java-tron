package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.p2p.dns.lookup.LookUpTxt;

public class InetUtilTest {

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
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("node.example.com", true)).thenReturn(mockAddr);
      List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
          Collections.singletonList("node.example.com:18888"));
      assertEquals(1, result.size());
      assertEquals("1.2.3.4", result.get(0).getAddress().getHostAddress());
      assertEquals(18888, result.get(0).getPort());
    }
  }

  @Test(timeout = 5000)
  public void testResolveListSingleDomainUnresolvable() {
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("bad.invalid", true)).thenReturn(null);
      mock.when(() -> LookUpTxt.lookUpIp("bad.invalid", false)).thenReturn(null);
      List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
          Collections.singletonList("bad.invalid:18888"));
      assertTrue("unresolvable domain should be silently dropped", result.isEmpty());
    }
  }

  @Test(timeout = 5000)
  public void testResolveListDomainFirstOrderPreservedBeforeIp() throws Exception {
    // Domain in position 0, IP literal in position 1 — verifies the final ordering loop
    // places the resolved domain before the IP literal.
    InetAddress domainAddr = InetAddress.getByName("3.3.3.3");
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("first.node", true)).thenReturn(domainAddr);
      List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
          Arrays.asList("first.node:18888", "10.0.0.2:8080"));
      assertEquals(2, result.size());
      assertEquals("3.3.3.3", result.get(0).getAddress().getHostAddress());
      assertEquals(18888, result.get(0).getPort());
      assertEquals("10.0.0.2", result.get(1).getAddress().getHostAddress());
      assertEquals(8080, result.get(1).getPort());
    }
  }

  @Test(timeout = 5000)
  public void testResolveListUnresolvableDomainFirstIpLiteralKept() {
    // Unresolvable domain in position 0 is dropped; trailing IP literal is kept.
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("bad.invalid", true)).thenReturn(null);
      mock.when(() -> LookUpTxt.lookUpIp("bad.invalid", false)).thenReturn(null);
      List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
          Arrays.asList("bad.invalid:18888", "1.1.1.1:8080"));
      assertEquals(1, result.size());
      assertEquals("1.1.1.1", result.get(0).getAddress().getHostAddress());
      assertEquals(8080, result.get(0).getPort());
    }
  }

  @Test(timeout = 5000)
  public void testResolveListMixedIpAndDomain() throws Exception {
    InetAddress domainAddr = InetAddress.getByName("5.5.5.5");
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("my.node", true)).thenReturn(domainAddr);
      List<InetSocketAddress> result = InetUtil.resolveInetSocketAddressList(
          Arrays.asList("192.168.0.1:18888", "my.node:8080", "10.0.0.1:9090"));
      assertEquals(3, result.size());
      assertEquals("192.168.0.1", result.get(0).getAddress().getHostAddress());
      assertEquals("5.5.5.5", result.get(1).getAddress().getHostAddress());
      assertEquals("10.0.0.1", result.get(2).getAddress().getHostAddress());
    }
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
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("peer.tron.network", true)).thenReturn(mockAddr);
      InetAddress result = InetUtil.resolveInetAddress("peer.tron.network");
      assertNotNull(result);
      assertEquals("3.3.3.3", result.getHostAddress());
    }
  }

  @Test(timeout = 5000)
  public void testResolveInetAddressDomainIpv4FallsBackToIpv6() throws Exception {
    InetAddress ipv6Addr = InetAddress.getByName("::1");
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("ipv6only.host", true)).thenReturn(null);
      mock.when(() -> LookUpTxt.lookUpIp("ipv6only.host", false)).thenReturn(ipv6Addr);
      InetAddress result = InetUtil.resolveInetAddress("ipv6only.host");
      assertNotNull(result);
    }
  }

  @Test(timeout = 5000)
  public void testResolveInetAddressUnresolvableReturnsNull() {
    try (MockedStatic<LookUpTxt> mock = mockStatic(LookUpTxt.class)) {
      mock.when(() -> LookUpTxt.lookUpIp("bad.invalid", true)).thenReturn(null);
      mock.when(() -> LookUpTxt.lookUpIp("bad.invalid", false)).thenReturn(null);
      InetAddress result = InetUtil.resolveInetAddress("bad.invalid");
      assertNull(result);
    }
  }
}
