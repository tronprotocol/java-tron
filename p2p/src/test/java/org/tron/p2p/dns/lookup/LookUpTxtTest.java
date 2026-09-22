package org.tron.p2p.dns.lookup;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;

public class LookUpTxtTest {

  @Test
  public void testJoinTXTRecord_singleString() throws TextParseException {
    Name name = Name.fromString("test.example.com.");
    TXTRecord record = new TXTRecord(name, DClass.IN, 300, "hello");
    Assert.assertEquals("hello", LookUpTxt.joinTXTRecord(record));
  }

  @Test
  public void testJoinTXTRecord_multipleStrings() throws TextParseException {
    Name name = Name.fromString("test.example.com.");
    TXTRecord record = new TXTRecord(name, DClass.IN, 300,
        Arrays.asList("enrtree-root:v1 ", "e=ABCDE ", "l=FGHIJ seq=1 sig=XYZ"));
    // joinTXTRecord trims each string before concatenating, so trailing spaces are removed
    Assert.assertEquals("enrtree-root:v1e=ABCDEl=FGHIJ seq=1 sig=XYZ",
        LookUpTxt.joinTXTRecord(record));
  }

  // -------------------------------------------------------------------------
  // lookUpIp tests
  // -------------------------------------------------------------------------

  /**
   * "localhost" is always present in /etc/hosts on every OS, so InetAddress.getByName resolves it
   * locally without issuing any DNS query — this validates the /etc/hosts fast path.
   */
  @Test
  @Ignore("might fail due to no netowrk")
  public void testLookUpIp_localhost_ipv4_resolvesViaHosts() {
    InetAddress address = LookUpTxt.lookUpIp("localhost", true);
    Assert.assertNotNull("localhost must resolve via /etc/hosts", address);
    Assert.assertTrue("Expected IPv4 loopback", address instanceof Inet4Address);
    Assert.assertTrue("Expected loopback address", address.isLoopbackAddress());
  }

  @Test
  @Ignore("might fail due to no netowrk")
  public void testLookUpIp_localhost_ipv6_resolvesViaHosts() {
    InetAddress address = LookUpTxt.lookUpIp("localhost", false);
    Assert.assertNotNull("localhost must resolve via /etc/hosts", address);
    Assert.assertTrue("Expected IPv6 loopback", address instanceof Inet6Address);
    Assert.assertTrue("Expected loopback address", address.isLoopbackAddress());
  }

  /**
   * example.com is a stable IANA-reserved domain that always has an A record.
   * This validates the normal DNS resolution path (Step 1 via OS resolver).
   */
  @Test
  @Ignore("might fail due to no netowrk")
  public void testLookUpIp_wellKnownDomain_ipv4_returnsNonNull() {
    InetAddress address = LookUpTxt.lookUpIp("example.com", true);
    Assert.assertNotNull("example.com should resolve to an IPv4 address", address);
    Assert.assertTrue("Expected Inet4Address", address instanceof Inet4Address);
  }

  /**
   * The ".invalid" TLD is RFC 2606-reserved and guaranteed never to resolve.
   * All three resolution steps (OS, default DNS, public DNS) should fail, returning null.
   */
  @Test
  public void testLookUpIp_nonexistentDomain_returnsNull() {
    int saved = LookUpTxt.maxRetryTimes;
    LookUpTxt.maxRetryTimes = 1;
    try {
      InetAddress address =
          LookUpTxt.lookUpIp("this.domain.absolutely.does.not.exist.invalid", true);
      Assert.assertNull("Non-existent domain should return null", address);
    } finally {
      LookUpTxt.maxRetryTimes = saved;
    }
  }
}
