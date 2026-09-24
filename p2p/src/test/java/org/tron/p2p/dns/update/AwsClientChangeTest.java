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
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;

/**
 * Covers the pure change-computation side of AwsClient — the part that decides which
 * Route53 records to create, update and delete for a published tree. None of it needs
 * the network: the SDK client is built lazily by the constructor and is never called
 * on these paths.
 *
 * <p>TTLs come from the Publish interface: rootTTL = 600s, treeNodeTTL = 7 days.
 */
public class AwsClientChangeTest {

  private static final String DOMAIN = "example.org";
  private static final long ROOT_TTL = 10 * 60;
  private static final long NODE_TTL = 7 * 24 * 60 * 60;

  private static AwsClient client;
  private static Method splitTxt;

  @BeforeClass
  public static void init() throws Exception {
    client = new AwsClient("access-key", "access-secret", "zone-id", "us-east-1", 0.1);
    splitTxt = AwsClient.class.getDeclaredMethod("splitTxt", String.class);
    splitTxt.setAccessible(true);
  }

  private static Map<String, String> records(String... keyValues) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put(keyValues[i], keyValues[i + 1]);
    }
    return map;
  }

  private static Change findByName(List<Change> changes, String name) {
    for (Change change : changes) {
      if (change.resourceRecordSet().name().equals(name)) {
        return change;
      }
    }
    return null;
  }

  @Test
  public void createsRecordsThatDoNotExistYet() {
    List<Change> changes = client.computeChanges(DOMAIN,
        records(DOMAIN, "root-value", "abc." + DOMAIN, "leaf-value"),
        new HashMap<>());

    Assert.assertEquals(2, changes.size());
    for (Change change : changes) {
      Assert.assertEquals(ChangeAction.CREATE, change.action());
    }
    // the root record gets the short root TTL; everything else gets the long one
    Assert.assertEquals(ROOT_TTL,
        findByName(changes, DOMAIN).resourceRecordSet().ttl().longValue());
    Assert.assertEquals(NODE_TTL,
        findByName(changes, "abc." + DOMAIN).resourceRecordSet().ttl().longValue());
  }

  @Test
  public void leavesUnchangedRecordsAlone() {
    Map<String, RecordSet> existing = new HashMap<>();
    // stored values are quoted, which is what splitTxt produces
    existing.put(DOMAIN, new RecordSet(new String[] {"\"root-value\""}, ROOT_TTL));

    List<Change> changes = client.computeChanges(DOMAIN, records(DOMAIN, "root-value"), existing);
    Assert.assertTrue("identical value and ttl must produce no change", changes.isEmpty());
  }

  @Test
  public void upsertsWhenValueChanges() {
    String leaf = "abc." + DOMAIN;
    Map<String, RecordSet> existing = new HashMap<>();
    existing.put(leaf, new RecordSet(new String[] {"\"old-value\""}, NODE_TTL));

    List<Change> changes = client.computeChanges(DOMAIN, records(leaf, "new-value"), existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.UPSERT, changes.get(0).action());
  }

  @Test
  public void upsertsWhenOnlyTtlChanges() {
    String leaf = "abc." + DOMAIN;
    Map<String, RecordSet> existing = new HashMap<>();
    // same value, wrong ttl — still needs writing back
    existing.put(leaf, new RecordSet(new String[] {"\"leaf-value\""}, NODE_TTL + 1));

    List<Change> changes = client.computeChanges(DOMAIN, records(leaf, "leaf-value"), existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.UPSERT, changes.get(0).action());
    Assert.assertEquals(NODE_TTL, changes.get(0).resourceRecordSet().ttl().longValue());
  }

  /**
   * Changing the root record takes an extra branch that tries to parse the old and new
   * values as RootEntry purely to log the transition, and swallows DnsException when
   * they are not parseable. Values carrying the tree-root-v1 prefix reach that parse
   * and fail inside it, so this covers the catch-and-continue path.
   *
   * <p>Note the parse is only safe here because both values are longer than the
   * 13-character prefix — see RootEntry.java:67, which substrings without a length
   * check and throws an unchecked StringIndexOutOfBoundsException on shorter input.
   * That escapes the DnsException catch in AwsClient.computeChanges and aborts the
   * whole publish. Pre-existing in libp2p, reported rather than fixed here.
   */
  @Test
  public void upsertsRootRecordAndIgnoresUnparseableRootEntry() {
    Map<String, RecordSet> existing = new HashMap<>();
    existing.put(DOMAIN, new RecordSet(new String[] {"\"tree-root-v1:AAAA\""}, ROOT_TTL));

    List<Change> changes = client.computeChanges(DOMAIN,
        records(DOMAIN, "tree-root-v1:BBBB"), existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.UPSERT, changes.get(0).action());
    Assert.assertEquals(ROOT_TTL, changes.get(0).resourceRecordSet().ttl().longValue());
  }

  @Test
  public void deletesRecordsNoLongerInTheTree() {
    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("stale." + DOMAIN, new RecordSet(new String[] {"\"gone\""}, NODE_TTL));

    List<Change> changes = client.makeDeletionChanges(new HashMap<>(), existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.DELETE, changes.get(0).action());
    Assert.assertEquals("stale." + DOMAIN, changes.get(0).resourceRecordSet().name());

    // a path that is still wanted must be kept
    Assert.assertTrue(
        client.makeDeletionChanges(records("stale." + DOMAIN, "still-here"), existing).isEmpty());
  }

  @Test
  public void computeChangesIncludesDeletions() {
    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("stale." + DOMAIN, new RecordSet(new String[] {"\"gone\""}, NODE_TTL));

    List<Change> changes = client.computeChanges(DOMAIN, records(DOMAIN, "root-value"), existing);
    Assert.assertEquals(2, changes.size());
    // CREATE must be ordered before DELETE
    Assert.assertEquals(ChangeAction.CREATE, changes.get(0).action());
    Assert.assertEquals(ChangeAction.DELETE, changes.get(1).action());
  }

  @Test
  public void sortsCreateBeforeUpsertBeforeDelete() {
    List<Change> changes = new ArrayList<>();
    changes.add(client.newTXTChange(ChangeAction.DELETE, "d." + DOMAIN, NODE_TTL, "\"v\""));
    changes.add(client.newTXTChange(ChangeAction.UPSERT, "u." + DOMAIN, NODE_TTL, "\"v\""));
    changes.add(client.newTXTChange(ChangeAction.CREATE, "c." + DOMAIN, NODE_TTL, "\"v\""));

    AwsClient.sortChanges(changes);

    Assert.assertEquals(ChangeAction.CREATE, changes.get(0).action());
    Assert.assertEquals(ChangeAction.UPSERT, changes.get(1).action());
    Assert.assertEquals(ChangeAction.DELETE, changes.get(2).action());
  }

  @Test
  public void sortsByNameWithinTheSameAction() {
    List<Change> changes = new ArrayList<>();
    changes.add(client.newTXTChange(ChangeAction.CREATE, "b." + DOMAIN, NODE_TTL, "\"v\""));
    changes.add(client.newTXTChange(ChangeAction.CREATE, "a." + DOMAIN, NODE_TTL, "\"v\""));

    AwsClient.sortChanges(changes);

    Assert.assertEquals("a." + DOMAIN, changes.get(0).resourceRecordSet().name());
    Assert.assertEquals("b." + DOMAIN, changes.get(1).resourceRecordSet().name());
  }

  @Test
  public void isSameChangeComparesActionNameAndValue() {
    Change a = client.newTXTChange(ChangeAction.CREATE, "a." + DOMAIN, NODE_TTL, "\"v\"");
    Change sameAsA = client.newTXTChange(ChangeAction.CREATE, "a." + DOMAIN, NODE_TTL, "\"v\"");
    Change otherName = client.newTXTChange(ChangeAction.CREATE, "b." + DOMAIN, NODE_TTL, "\"v\"");
    Change otherAction = client.newTXTChange(ChangeAction.DELETE, "a." + DOMAIN, NODE_TTL, "\"v\"");

    Assert.assertTrue(AwsClient.isSameChange(a, sameAsA));
    Assert.assertFalse(AwsClient.isSameChange(a, otherName));
    Assert.assertFalse(AwsClient.isSameChange(a, otherAction));
  }

  @Test
  public void splitTxtQuotesAndChunksAt253Chars() throws Exception {
    // a short value is simply wrapped in quotes
    Assert.assertEquals("\"abc\"", splitTxt.invoke(client, "abc"));
    Assert.assertEquals("", splitTxt.invoke(client, ""));

    // TXT strings cap at 255 bytes including the two quotes, so the payload is
    // chunked every 253 characters and each chunk is quoted separately
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 300; i++) {
      sb.append('x');
    }
    String result = (String) splitTxt.invoke(client, sb.toString());

    String first = sb.substring(0, 253);
    String second = sb.substring(253);
    Assert.assertEquals("\"" + first + "\"" + "\"" + second + "\"", result);

    // exactly 253 characters must stay a single chunk
    String exact = sb.substring(0, 253);
    Assert.assertEquals("\"" + exact + "\"", splitTxt.invoke(client, exact));
  }
}
