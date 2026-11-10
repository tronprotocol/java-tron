package org.tron.p2p.dns.lookup;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class LookUpTxtTest {

  private static final int[] TYPES = {Type.TXT, Type.A, Type.AAAA};
  private static final long TTL = Integer.MAX_VALUE;
  private Cache previousCache;
  private Cache sharedCache;

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

  @Before
  public void setUp() {
    previousCache = Lookup.getDefaultCache(DClass.IN);
    sharedCache = new Cache(DClass.IN);
    Lookup.setDefaultCache(sharedCache, DClass.IN);
  }

  @After
  public void tearDown() {
    Lookup.setDefaultCache(previousCache, DClass.IN);
  }

  @Test
  public void longLivedNegativeAnswerDoesNotSuppressTheNextResolver() throws Exception {
    for (int type : TYPES) {
      Name name = Name.fromString("retry-" + type + ".example.invalid.");
      Lookup lookup = LookUpTxt.createLookup(name.toString(), type);
      StubResolver missing = new StubResolver(null);
      lookup.setResolver(missing);

      Assert.assertNull(lookup.run());
      Assert.assertEquals(Lookup.HOST_NOT_FOUND, lookup.getResult());
      Assert.assertEquals(1, missing.calls);

      Record healthy = answer(name, type, false);
      assertAnswer(lookup, new StubResolver(healthy), healthy);
    }
  }

  @Test
  public void positiveAnswerDoesNotPoisonLaterLookupsOrTheSharedCache() throws Exception {
    for (int type : TYPES) {
      Name name = Name.fromString("positive-" + type + ".example.invalid.");
      Record bad = answer(name, type, true);
      assertAnswer(LookUpTxt.createLookup(name.toString(), type), new StubResolver(bad), bad);

      Record healthy = answer(name, type, false);
      assertAnswer(LookUpTxt.createLookup(name.toString(), type),
          new StubResolver(healthy), healthy);
      Assert.assertTrue(sharedCache.lookupRecords(name, type, Credibility.NORMAL).isUnknown());
    }
  }

  @Test
  public void poisonedSharedCacheDoesNotAffectNewLookups() throws Exception {
    for (int type : TYPES) {
      for (boolean negative : new boolean[]{false, true}) {
        Name name = Name.fromString("shared-" + type + "-" + negative + ".example.invalid.");
        StubResolver poison = new StubResolver(negative ? null : answer(name, type, true));
        Message query = Message.newQuery(Record.newRecord(name, type, DClass.IN));
        sharedCache.addMessage(poison.send(query));
        Assert.assertFalse(sharedCache.lookupRecords(name, type, Credibility.NORMAL).isUnknown());

        Record healthy = answer(name, type, false);
        assertAnswer(LookUpTxt.createLookup(name.toString(), type),
            new StubResolver(healthy), healthy);

        Assert.assertSame(sharedCache, Lookup.getDefaultCache(DClass.IN));
        Assert.assertFalse(sharedCache.lookupRecords(name, type, Credibility.NORMAL).isUnknown());
      }
    }
  }

  private static void assertAnswer(Lookup lookup, StubResolver resolver, Record expected) {
    lookup.setResolver(resolver);
    Record[] records = lookup.run();
    Assert.assertNotNull(records);
    Assert.assertEquals(1, records.length);
    Assert.assertEquals(expected, records[0]);
    Assert.assertEquals(1, resolver.calls);
  }

  private static Record answer(Name name, int type, boolean bad) throws Exception {
    switch (type) {
      case Type.TXT:
        return new TXTRecord(name, DClass.IN, TTL, bad ? "forged TXT" : "healthy TXT");
      case Type.A:
        return new ARecord(name, DClass.IN, TTL,
            InetAddress.getByName(bad ? "192.0.2.66" : "192.0.2.1"));
      case Type.AAAA:
        return new AAAARecord(name, DClass.IN, TTL,
            InetAddress.getByName(bad ? "2001:db8::66" : "2001:db8::1"));
      default:
        throw new AssertionError("Unexpected record type: " + type);
    }
  }

  private static class StubResolver extends SimpleResolver {
    private final Record answer;
    private int calls;

    private StubResolver(Record answer) throws UnknownHostException {
      super("127.0.0.1");
      this.answer = answer;
    }

    @Override
    public Message send(Message query) {
      calls++;
      Message response = new Message(query.getHeader().getID());
      response.getHeader().setFlag(Flags.QR);
      response.getHeader().setFlag(Flags.AA);
      response.addRecord(query.getQuestion(), Section.QUESTION);
      if (answer == null) {
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        Name zone = Name.fromConstantString("example.invalid.");
        response.addRecord(new SOARecord(zone, DClass.IN, TTL,
            Name.fromConstantString("ns.example.invalid."),
            Name.fromConstantString("admin.example.invalid."),
            1, 3600, 60, 86400, TTL), Section.AUTHORITY);
      } else {
        response.addRecord(answer, Section.ANSWER);
      }
      return response;
    }
  }
}
