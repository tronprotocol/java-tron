package org.tron.p2p.dns.update;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.dns.update.AwsClient.RecordSet;
import org.tron.p2p.exception.DnsException;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;

/**
 * The batching side of AwsClient: Route53 caps a change batch at 32000 bytes of
 * RDATA and 1000 changes, and counts an UPSERT as two. Getting the split wrong
 * means a publish is rejected wholesale by the API.
 */
public class AwsClientBatchTest {

  private static AwsClient client;
  private static Method splitChanges;

  @BeforeClass
  public static void init() throws Exception {
    client = new AwsClient("access-key", "access-secret", "zone-id", "us-east-1", 0.1);
    splitChanges = AwsClient.class.getDeclaredMethod(
        "splitChanges", List.class, int.class, int.class);
    splitChanges.setAccessible(true);
  }

  @SuppressWarnings("unchecked")
  private static List<List<Change>> split(List<Change> changes, int sizeLimit, int countLimit)
      throws Exception {
    return (List<List<Change>>) splitChanges.invoke(null, changes, sizeLimit, countLimit);
  }

  private static String repeat(char c, int n) {
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      sb.append(c);
    }
    return sb.toString();
  }

  @Test
  public void constructorRejectsMissingCredentials() {
    for (String[] pair : new String[][] {{null, "secret"}, {"", "secret"},
        {"key", null}, {"key", ""}}) {
      try {
        new AwsClient(pair[0], pair[1], "zone-id", "us-east-1", 0.1);
        Assert.fail("expected a DnsException for " + pair[0] + "/" + pair[1]);
      } catch (DnsException expected) {
        Assert.assertTrue(expected.getMessage().contains("Access Key"));
      }
    }
  }

  @Test
  public void everythingFitsInOneBatch() throws Exception {
    List<Change> changes = new ArrayList<>();
    changes.add(client.newTXTChange(ChangeAction.CREATE, "a.example.org", 600, "aaa"));
    changes.add(client.newTXTChange(ChangeAction.CREATE, "b.example.org", 600, "bbb"));

    List<List<Change>> batches = split(changes, 32000, 1000);
    Assert.assertEquals(1, batches.size());
    Assert.assertEquals(2, batches.get(0).size());
  }

  @Test
  public void sizeLimitStartsANewBatch() throws Exception {
    List<Change> changes = new ArrayList<>();
    changes.add(client.newTXTChange(ChangeAction.CREATE, "a.example.org", 600, repeat('a', 60)));
    changes.add(client.newTXTChange(ChangeAction.CREATE, "b.example.org", 600, repeat('b', 60)));

    // 60 bytes each, so a 100-byte limit admits exactly one per batch.
    List<List<Change>> batches = split(changes, 100, 1000);
    Assert.assertEquals(2, batches.size());
    Assert.assertEquals(1, batches.get(0).size());
    Assert.assertEquals(1, batches.get(1).size());
  }

  @Test
  public void countLimitStartsANewBatch() throws Exception {
    List<Change> changes = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      changes.add(client.newTXTChange(ChangeAction.CREATE, "n" + i + ".example.org", 600, "x"));
    }

    List<List<Change>> batches = split(changes, 32000, 2);
    Assert.assertEquals(3, batches.size());
    Assert.assertEquals(2, batches.get(0).size());
    Assert.assertEquals(2, batches.get(1).size());
    Assert.assertEquals(1, batches.get(2).size());
  }

  @Test
  public void upsertCountsAsTwoChanges() throws Exception {
    List<Change> changes = new ArrayList<>();
    changes.add(client.newTXTChange(ChangeAction.UPSERT, "a.example.org", 600, "x"));
    changes.add(client.newTXTChange(ChangeAction.UPSERT, "b.example.org", 600, "x"));

    // Route53 bills an UPSERT as a delete plus a create, so a count limit of 2
    // fits only one of them.
    List<List<Change>> batches = split(changes, 32000, 2);
    Assert.assertEquals(2, batches.size());
  }

  @Test
  public void emptyInputProducesNoBatches() throws Exception {
    Assert.assertTrue(split(new ArrayList<Change>(), 32000, 1000).isEmpty());
  }

  @Test
  public void multiValueChangeSumsItsRecordSizes() throws Exception {
    List<Change> changes = new ArrayList<>();
    changes.add(client.newTXTChange(ChangeAction.CREATE, "a.example.org", 600,
        repeat('a', 30), repeat('b', 30)));
    changes.add(client.newTXTChange(ChangeAction.CREATE, "b.example.org", 600, "x"));

    // The first change is 60 bytes across two records, so it alone fills a
    // 60-byte budget.
    List<List<Change>> batches = split(changes, 60, 1000);
    Assert.assertEquals(2, batches.size());
  }

  @Test
  public void makeDeletionChangesTargetsOnlyRecordsNoLongerKept() {
    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("keep.example.org", new RecordSet(new String[] {"a"}, 600));
    existing.put("drop.example.org", new RecordSet(new String[] {"b"}, 600));

    Map<String, String> keeps = new HashMap<>();
    keeps.put("keep.example.org", "a");

    List<Change> deletions = client.makeDeletionChanges(keeps, existing);
    Assert.assertEquals(1, deletions.size());
    Change deletion = deletions.get(0);
    Assert.assertEquals(ChangeAction.DELETE, deletion.action());
    Assert.assertEquals("drop.example.org", deletion.resourceRecordSet().name());
    Assert.assertEquals(600L, deletion.resourceRecordSet().ttl().longValue());
  }

  @Test
  public void makeDeletionChangesIsEmptyWhenEverythingIsKept() {
    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("keep.example.org", new RecordSet(new String[] {"a"}, 600));
    Map<String, String> keeps = new HashMap<>();
    keeps.put("keep.example.org", "a");

    Assert.assertTrue(client.makeDeletionChanges(keeps, existing).isEmpty());
  }

  @Test
  public void isSubdomainIgnoresTrailingDots() {
    Assert.assertTrue(AwsClient.isSubdomain("a.example.org", "example.org"));
    Assert.assertTrue(AwsClient.isSubdomain("a.example.org.", "example.org"));
    Assert.assertTrue(AwsClient.isSubdomain("a.example.org", "example.org."));
    Assert.assertTrue(AwsClient.isSubdomain("example.org", "example.org"));
    Assert.assertFalse(AwsClient.isSubdomain("example.org", "a.example.org"));
    Assert.assertFalse(AwsClient.isSubdomain("a.example.com", "example.org"));
    // Label boundaries are respected: notexample.org is not under example.org.
    Assert.assertFalse(AwsClient.isSubdomain("notexample.org", "example.org"));
  }

  @Test
  public void newTxtChangeCarriesEveryValue() {
    Change change = client.newTXTChange(ChangeAction.UPSERT, "a.example.org", 42, "one", "two");
    Assert.assertEquals(ChangeAction.UPSERT, change.action());
    Assert.assertEquals("a.example.org", change.resourceRecordSet().name());
    Assert.assertEquals(42L, change.resourceRecordSet().ttl().longValue());
    Assert.assertEquals(2, change.resourceRecordSet().resourceRecords().size());
    Assert.assertEquals("one", change.resourceRecordSet().resourceRecords().get(0).value());
    Assert.assertEquals("two", change.resourceRecordSet().resourceRecords().get(1).value());
  }
}
