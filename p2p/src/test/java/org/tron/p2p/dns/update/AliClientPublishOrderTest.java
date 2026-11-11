package org.tron.p2p.dns.update;

import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.TreeTest;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.exception.DnsException;

public class AliClientPublishOrderTest {

  private static final String DOMAIN = "nodes.example.invalid";

  @Test
  public void publishesDependenciesThenRootThenDeletesOldRecords() throws Exception {
    for (boolean update : new boolean[]{false, true}) {
      RecordingClient client = new RecordingClient();
      Tree tree = tree();
      String dependency = dependency(tree);
      if (update) {
        client.existing.put("@", record("@", "old root"));
        client.existing.put(dependency, record(dependency, tree.toTXT(null).get(dependency)));
        client.existing.put("stale", record("stale", "old node"));
      }

      client.deploy(DOMAIN, tree);

      assertRootLast(client, tree, update ? "update:@" : "add:@");
      Assert.assertEquals(tree.toTXT(null).size() + (update ? 1 : 0), client.calls.size());
      if (update) {
        Assert.assertTrue(client.calls.contains("update:" + dependency));
        Assert.assertEquals("delete:stale", client.calls.get(client.calls.size() - 1));
      }
    }
  }

  @Test
  public void failedWritesDoNotAdvancePublicationOrDeleteOldRecords() throws Exception {
    for (boolean rootFailure : new boolean[]{false, true}) {
      for (boolean update : new boolean[]{false, true}) {
        for (boolean throwFailure : new boolean[]{false, true}) {
          RecordingClient client = new RecordingClient();
          Tree tree = tree();
          client.failedName = rootFailure ? "@" : dependency(tree);
          client.throwFailure = throwFailure;
          client.existing.put("stale", record("stale", "old node"));
          if (update) {
            client.existing.put("@", record("@", "old root"));
            client.existing.put(client.failedName, record(client.failedName, "old record"));
          }

          DnsException error = Assert.assertThrows(DnsException.class,
              () -> client.deploy(DOMAIN, tree));

          Assert.assertEquals(DnsException.TypeEnum.DEPLOY_DOMAIN_FAILED, error.getType());
          Assert.assertTrue(client.calls.contains((update ? "update:" : "add:")
              + client.failedName));
          Assert.assertFalse(client.calls.contains("delete:stale"));
          if (rootFailure) {
            assertRootLast(client, tree, update ? "update:@" : "add:@");
            Assert.assertEquals(tree.toTXT(null).size(), client.calls.size());
          } else {
            Assert.assertFalse(client.calls.contains("add:@"));
            Assert.assertFalse(client.calls.contains("update:@"));
          }
        }
      }
    }
  }

  private static Tree tree() throws Exception {
    return TreeTest.signedTree(new Tree() {
      @Override
      public Map<String, String> toTXT(String rootDomain) {
        Map<String, String> records = super.toTXT(rootDomain);
        Map<String, String> rootFirst = new LinkedHashMap<>();
        rootFirst.put("@", records.get("@"));
        rootFirst.putAll(records);
        return rootFirst;
      }
    }, "192.0.2.1", "192.0.2.2");
  }

  private static String dependency(Tree tree) {
    return tree.toTXT(null).keySet().stream().filter(name -> !name.equals("@"))
        .findFirst().get();
  }

  private static DescribeDomainRecordsResponseBodyDomainRecordsRecord record(
      String name, String value) {
    return new DescribeDomainRecordsResponseBodyDomainRecordsRecord()
        .setRecordId(name).setValue(value).setTTL(0L);
  }

  private static void assertRootLast(RecordingClient client, Tree tree, String rootCall) {
    Assert.assertEquals(tree.toTXT(null).size() - 1, client.calls.indexOf(rootCall));
    for (String name : tree.toTXT(null).keySet()) {
      Assert.assertTrue(client.calls.contains("add:" + name)
          || client.calls.contains("update:" + name));
    }
  }

  private static class RecordingClient extends AliClient {
    private final Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> existing =
        new HashMap<>();
    private final List<String> calls = new ArrayList<>();
    private String failedName;
    private boolean throwFailure;

    private RecordingClient() throws Exception {
      super("alidns.aliyuncs.com", "test-id", "test-secret", 0.1);
    }

    @Override
    public Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> collectRecords(
        String domain) {
      return existing;
    }

    @Override
    public boolean addRecord(String domain, String name, String value, long ttl) throws Exception {
      return write("add:", name);
    }

    @Override
    public boolean updateRecord(String id, String name, String value, long ttl) throws Exception {
      return write("update:", name);
    }

    @Override
    public boolean deleteRecord(String id) {
      calls.add("delete:" + id);
      return true;
    }

    private boolean write(String action, String name) throws IOException {
      calls.add(action + name);
      if (name.equals(failedName)) {
        if (throwFailure) {
          throw new IOException("Injected write failure");
        }
        return false;
      }
      return true;
    }
  }
}
