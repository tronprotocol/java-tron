package org.tron.p2p.dns.update;

import com.aliyun.alidns20150109.Client;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsRequest;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponse;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecords;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord;
import com.aliyun.teaopenapi.models.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.RootEntry;
import org.tron.p2p.dns.tree.Tree;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

@RunWith(Parameterized.class)
public class DnsSignerPublicationTest {

  private static final String DOMAIN = "nodes.example.invalid";
  private static final String OLD_KEY = "01";
  private static final String NEW_KEY = "02";
  private static final String LINK =
      "tree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@linked.example.invalid";
  private final Map<String, String> records = new HashMap<>();
  private final List<String> writes = new ArrayList<>();
  private Publish<?> client;
  private boolean failRootWrite;

  @Parameterized.Parameter
  public String provider;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> providers() {
    return Arrays.asList(new Object[][]{{"Aliyun"}, {"Route53"}});
  }

  @Before
  public void setUp() throws Exception {
    client = provider.equals("Aliyun") ? aliClient() : awsClient();
  }

  @Test
  public void signerChangePublishesOnceWithUnchangedNodesAndLinks() throws Exception {
    Tree published = tree(OLD_KEY, 1);
    load(published);
    Tree updated = tree(NEW_KEY, 1);

    client.deploy(DOMAIN, updated);

    Assert.assertEquals(published.getRootEntry().getERoot(), updated.getRootEntry().getERoot());
    Assert.assertEquals(published.getRootEntry().getLRoot(), updated.getRootEntry().getLRoot());
    Assert.assertEquals(Collections.singletonList("@"), writes);
    Assert.assertEquals(8, root().getSeq());
    assertSigner(NEW_KEY, true);
    assertSigner(OLD_KEY, false);

    writes.clear();
    client.deploy(DOMAIN, tree(NEW_KEY, 1));

    Assert.assertTrue(writes.isEmpty());
    Assert.assertEquals(updated.getRootEntry().toFormat(), root().toFormat());
  }

  @Test
  public void sameSignerWithNewSequenceAndSignatureDoesNotPublish() throws Exception {
    // Include a leading-zero public key: identity must not depend on URL encoding.
    for (String key : new String[]{OLD_KEY, "2d"}) {
      Tree published = tree(key, 1);
      load(published);
      Tree updated = tree(key, 1);

      client.deploy(DOMAIN, updated);

      Assert.assertEquals(8, updated.getSeq());
      Assert.assertNotEquals(published.getRootEntry().toFormat(),
          updated.getRootEntry().toFormat());
      Assert.assertTrue(writes.isEmpty());
      Assert.assertEquals(published.getRootEntry().toFormat(), root().toFormat());
      assertSigner(key, true);
    }
  }

  @Test
  public void nodeChangesBelowThresholdOnlyPublishForLinkOrSignerChanges() throws Exception {
    for (String[] links : new String[][]{{}, {LINK}}) {
      for (String key : new String[]{OLD_KEY, NEW_KEY}) {
        Tree published = tree(OLD_KEY, 11);
        load(published);
        Tree updated = tree(key, 12, links);

        client.deploy(DOMAIN, updated);

        boolean changed = key.equals(NEW_KEY) || links.length > 0;
        Assert.assertEquals(changed, !writes.isEmpty());
        Assert.assertEquals((changed ? updated : published).getRootEntry().toFormat(),
            root().toFormat());
        assertSigner(key, true);
      }
    }
  }

  @Test
  public void linkOnlyChangesPublishOnceWithUnchangedNodesAndSigner() throws Exception {
    for (boolean addLink : new boolean[]{false, true}) {
      Tree withLinks = tree(OLD_KEY, 1, LINK);
      Tree withoutLinks = tree(OLD_KEY, 1);
      Tree published = addLink ? withoutLinks : withLinks;
      Tree updated = addLink ? withLinks : withoutLinks;
      load(published);

      client.deploy(DOMAIN, updated);

      Assert.assertEquals(published.getRootEntry().getERoot(), updated.getRootEntry().getERoot());
      Assert.assertNotEquals(published.getRootEntry().getLRoot(),
          updated.getRootEntry().getLRoot());
      Assert.assertEquals(updated.toTXT(null), records);
      Assert.assertTrue(writes.contains("@"));
      Assert.assertEquals(8, root().getSeq());
      assertSigner(OLD_KEY, true);

      writes.clear();
      client.deploy(DOMAIN, tree(OLD_KEY, 1, addLink ? new String[]{LINK} : new String[0]));

      Assert.assertTrue(writes.isEmpty());
      Assert.assertEquals(updated.getRootEntry().toFormat(), root().toFormat());
    }
  }

  @Test
  public void sameSignerStillPublishesNodeChangesAtThreshold() throws Exception {
    load(tree(OLD_KEY, 10));
    Tree updated = tree(OLD_KEY, 11);

    client.deploy(DOMAIN, updated);

    Assert.assertEquals(updated.getRootEntry().toFormat(), root().toFormat());
    Assert.assertTrue(writes.contains("@"));
    assertSigner(OLD_KEY, true);
  }

  @Test
  public void failedLinkOrSignerChangeCanRetry() throws Exception {
    for (boolean changeSigner : new boolean[]{false, true}) {
      Tree published = tree(OLD_KEY, 1);
      load(published);
      String key = changeSigner ? NEW_KEY : OLD_KEY;
      Tree updated = tree(key, 1, changeSigner ? new String[0] : new String[]{LINK});
      failRootWrite = true;

      Assert.assertThrows(Exception.class, () -> client.deploy(DOMAIN, updated));

      Assert.assertEquals(published.getRootEntry().toFormat(), root().toFormat());
      failRootWrite = false;
      client.deploy(DOMAIN, updated);

      Assert.assertEquals(8, root().getSeq());
      assertSigner(key, true);
      Assert.assertEquals(updated.toTXT(null), records);
    }
  }

  @Test
  public void missingRootDoesNotReusePreviouslyCollectedRoot() throws Exception {
    load(tree(OLD_KEY, 1, LINK));
    client.collectRecords(DOMAIN);
    records.remove("@");

    client.deploy(DOMAIN, tree(OLD_KEY, 1, LINK));

    Assert.assertEquals(Collections.singletonList("@"), writes);
    assertSigner(OLD_KEY, true);
  }

  @Test
  public void unrecoverableSignerIsReplacedOnce() throws Exception {
    // Cover an invalid recovery header and invalid curve data with a valid header.
    for (int recoveryId : new int[]{42, 27}) {
      load(tree(OLD_KEY, 1));
      client.collectRecords(DOMAIN);
      corruptRootSignature(recoveryId);
      Tree updated = tree(OLD_KEY, 1);

      client.deploy(DOMAIN, updated);

      Assert.assertEquals(Collections.singletonList("@"), writes);
      Assert.assertEquals(8, root().getSeq());
      assertSigner(OLD_KEY, true);

      writes.clear();
      client.deploy(DOMAIN, tree(OLD_KEY, 1));

      Assert.assertTrue(writes.isEmpty());
      Assert.assertEquals(updated.getRootEntry().toFormat(), root().toFormat());
    }
  }

  @Test
  public void failedUnrecoverableSignerReplacementCanRetry() throws Exception {
    load(tree(OLD_KEY, 1));
    String corrupted = corruptRootSignature(42);
    failRootWrite = true;

    Assert.assertThrows(Exception.class, () -> client.deploy(DOMAIN, tree(OLD_KEY, 1)));

    Assert.assertEquals(corrupted, records.get("@"));
    Assert.assertTrue(writes.isEmpty());
    failRootWrite = false;
    client.deploy(DOMAIN, tree(OLD_KEY, 1));

    Assert.assertEquals(Collections.singletonList("@"), writes);
    Assert.assertEquals(8, root().getSeq());
    assertSigner(OLD_KEY, true);
  }

  private String corruptRootSignature(int recoveryId) throws Exception {
    RootEntry root = root();
    byte[] signature = new byte[65];
    signature[64] = (byte) recoveryId;
    root.setSignature(signature);
    String corrupted = root.toFormat();
    records.put("@", corrupted);
    return corrupted;
  }

  private void assertSigner(String privateKey, boolean expected) throws Exception {
    RootEntry root = root();
    String publicKey = Algorithm.generateKeyPair(privateKey).getPublicKey().toString(16);
    Assert.assertEquals(expected,
        Algorithm.verifySignature(publicKey, root.toString(), root.getSignature()));
  }

  private static Tree tree(String privateKey, int nodeCount, String... links) throws Exception {
    List<String> nodes = new ArrayList<>();
    for (int i = 0; i < nodeCount; i++) {
      nodes.add(Entry.nodesPrefix + DnsNode.compress(Collections.singletonList(
          new DnsNode(null, "192.0.2." + (i + 1), null, 18888))));
    }
    Tree tree = new Tree();
    tree.makeTree(7, nodes, Arrays.asList(links), privateKey);
    return tree;
  }

  private static void replaceField(Object target, Class<?> owner, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private void load(Tree tree) {
    records.clear();
    records.putAll(tree.toTXT(null));
    writes.clear();
  }

  private RootEntry root() throws Exception {
    return RootEntry.parseEntry(records.get("@"));
  }

  private void write(String name, String value) {
    if (name.equals("@") && failRootWrite) {
      throw new IllegalStateException("Injected root write failure");
    }
    writes.add(name);
    records.put(name, value);
  }

  private AliClient aliClient() throws Exception {
    AliClient client = new AliClient("alidns.aliyuncs.com", "test-id", "test-secret", 0.1) {
      @Override
      public boolean addRecord(String domain, String name, String value, long ttl) {
        write(name, value);
        return true;
      }

      @Override
      public boolean updateRecord(String id, String name, String value, long ttl) {
        write(name, value);
        return true;
      }

      @Override
      public boolean deleteRecord(String id) {
        writes.add(id);
        records.remove(id);
        return true;
      }
    };
    Client sdkClient = new Client(new Config().setAccessKeyId("test-id")
        .setAccessKeySecret("test-secret").setEndpoint("alidns.aliyuncs.com")) {
      @Override
      public DescribeDomainRecordsResponse describeDomainRecords(
          DescribeDomainRecordsRequest request) {
        List<DescribeDomainRecordsResponseBodyDomainRecordsRecord> responseRecords =
            new ArrayList<>();
        records.forEach((name, value) -> responseRecords.add(
            new DescribeDomainRecordsResponseBodyDomainRecordsRecord().setRR(name)
                .setRecordId(name).setValue(value)
                .setTTL(name.equals("@") ? (long) Publish.rootTTL : 24L * 60 * 60)));
        return new DescribeDomainRecordsResponse().setStatusCode(200)
            .setBody(new DescribeDomainRecordsResponseBody()
                .setTotalCount((long) responseRecords.size())
                .setDomainRecords(new DescribeDomainRecordsResponseBodyDomainRecords()
                    .setRecord(responseRecords)));
      }
    };
    replaceField(client, AliClient.class, "aliDnsClient", sdkClient);
    return client;
  }

  private AwsClient awsClient() throws Exception {
    AwsClient client = new AwsClient("test-id", "test-secret", "ZTEST", "us-east-1", 0.1) {
      @Override
      public void submitChanges(List<Change> changes, String comment) {
        for (Change change : changes) {
          ResourceRecordSet record = change.resourceRecordSet();
          String name = record.name().equals(DOMAIN) ? "@"
              : record.name().substring(0, record.name().length() - DOMAIN.length() - 1);
          if (change.action() == ChangeAction.DELETE) {
            writes.add(name);
            records.remove(name);
          } else {
            StringBuilder value = new StringBuilder();
            record.resourceRecords().forEach(part ->
                value.append(part.value().replace("\"", "")));
            write(name, value.toString());
          }
        }
      }
    };
    Field sdkField = AwsClient.class.getDeclaredField("route53Client");
    sdkField.setAccessible(true);
    ((Route53Client) sdkField.get(client)).close();
    Route53Client sdkClient = (Route53Client) Proxy.newProxyInstance(
        Route53Client.class.getClassLoader(), new Class<?>[]{Route53Client.class},
        (proxy, method, args) -> {
          if (!method.getName().equals("listResourceRecordSets")) {
            throw new AssertionError("Unexpected Route53 call: " + method.getName());
          }
          List<ResourceRecordSet> responseRecords = new ArrayList<>();
          records.forEach((name, value) -> responseRecords.add(ResourceRecordSet.builder()
              .name(name.equals("@") ? DOMAIN : name + "." + DOMAIN).type(RRType.TXT)
              .ttl(name.equals("@") ? (long) Publish.rootTTL : Publish.treeNodeTTL)
              .resourceRecords(ResourceRecord.builder().value("\"" + value + "\"").build())
              .build()));
          return ListResourceRecordSetsResponse.builder().isTruncated(false)
              .resourceRecordSets(responseRecords).build();
        });
    replaceField(client, AwsClient.class, "route53Client", sdkClient);
    return client;
  }
}
