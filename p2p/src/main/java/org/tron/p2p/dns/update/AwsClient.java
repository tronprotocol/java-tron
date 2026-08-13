package org.tron.p2p.dns.update;


import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.dns.tree.LinkEntry;
import org.tron.p2p.dns.tree.NodesEntry;
import org.tron.p2p.dns.tree.RootEntry;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.GetChangeRequest;
import software.amazon.awssdk.services.route53.model.GetChangeResponse;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesByNameRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesByNameResponse;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

@Slf4j(topic = "net")
public class AwsClient implements Publish {

  // Route53 limits change sets to 32k of 'RDATA size'. Change sets are also limited to
  // 1000 items. UPSERTs count double.
  // https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/DNSLimitations.html#limits-api-requests-changeresourcerecordsets
  public static final int route53ChangeSizeLimit = 32000;
  public static final int route53ChangeCountLimit = 1000;
  public static final int maxRetryLimit = 60;
  private int lastSeq = 0;
  private Route53Client route53Client;
  private String zoneId;
  private Set<DnsNode> serverNodes;
  private static final String symbol = "\"";
  private static final String postfix = ".";
  private double changeThreshold;

  public AwsClient(final String accessKey, final String accessKeySecret,
      final String zoneId, final String region, double changeThreshold) throws DnsException {
    if (StringUtils.isEmpty(accessKey) || StringUtils.isEmpty(accessKeySecret)) {
      throw new DnsException(TypeEnum.DEPLOY_DOMAIN_FAILED,
          "Need Route53 Access Key ID and secret to proceed");
    }
    StaticCredentialsProvider staticCredentialsProvider = StaticCredentialsProvider.create(
        new AwsCredentials() {
          @Override
          public String accessKeyId() {
            return accessKey;
          }

          @Override
          public String secretAccessKey() {
            return accessKeySecret;
          }
        });
    route53Client = Route53Client.builder()
        .credentialsProvider(staticCredentialsProvider)
        .region(Region.of(region))
        .build();
    this.zoneId = zoneId;
    this.serverNodes = new HashSet<>();
    this.changeThreshold = changeThreshold;
  }

  private void checkZone(String domain) {
    if (StringUtils.isEmpty(this.zoneId)) {
      this.zoneId = findZoneID(domain);
    }
  }

  private String findZoneID(String domain) {
    log.info("Finding Route53 Zone ID for {}", domain);
    ListHostedZonesByNameRequest.Builder request = ListHostedZonesByNameRequest.builder();
    while (true) {
      ListHostedZonesByNameResponse response = route53Client.listHostedZonesByName(request.build());
      for (HostedZone hostedZone : response.hostedZones()) {
        if (isSubdomain(domain, hostedZone.name())) {
          // example: /hostedzone/Z0404776204LVYA8EZNVH
          return hostedZone.id().split("/")[2];
        }
      }
      if (Boolean.FALSE.equals(response.isTruncated())) {
        break;
      }
      request.dnsName(response.dnsName());
      request.hostedZoneId(response.nextHostedZoneId());
    }
    return null;
  }

  @Override
  public void testConnect() throws Exception {
    ListHostedZonesByNameRequest.Builder request = ListHostedZonesByNameRequest.builder();
    while (true) {
      ListHostedZonesByNameResponse response = route53Client.listHostedZonesByName(request.build());
      if (Boolean.FALSE.equals(response.isTruncated())) {
        break;
      }
      request.dnsName(response.dnsName());
      request.hostedZoneId(response.nextHostedZoneId());
    }
  }

  // uploads the given tree to Route53.
  @Override
  public void deploy(String domain, Tree tree) throws Exception {
    checkZone(domain);

    Map<String, RecordSet> existing = collectRecords(domain);
    log.info("Find {} TXT records, {} nodes for {}", existing.size(), serverNodes.size(), domain);
    String represent = LinkEntry.buildRepresent(tree.getBase32PublicKey(), domain);
    log.info("Trying to publish {}", represent);

    tree.setSeq(this.lastSeq + 1);
    tree.sign(); //seq changed, wo need to sign again
    Map<String, String> records = tree.toTXT(domain);

    List<Change> changes = computeChanges(domain, records, existing);

    Set<DnsNode> treeNodes = new HashSet<>(tree.getDnsNodes());
    treeNodes.removeAll(serverNodes); // tree - dns
    int addNodeSize = treeNodes.size();

    Set<DnsNode> set1 = new HashSet<>(serverNodes);
    treeNodes = new HashSet<>(tree.getDnsNodes());
    set1.removeAll(treeNodes); // dns - tree
    int deleteNodeSize = set1.size();

    if (serverNodes.isEmpty()
        || (addNodeSize + deleteNodeSize) / (double) serverNodes.size() >= changeThreshold) {
      String comment = String.format("Tree update of %s at seq %d", domain, tree.getSeq());
      log.info(comment);
      submitChanges(changes, comment);
    } else {
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(4);
      double changePercent = (addNodeSize + deleteNodeSize) / (double) serverNodes.size();
      log.info("Sum of node add & delete percent {} is below changeThreshold {}, skip this changes",
          nf.format(changePercent), changeThreshold);
    }
    serverNodes.clear();
  }

  // removes all TXT records of the given domain.
  @Override
  public boolean deleteDomain(String rootDomain) throws Exception {
    checkZone(rootDomain);

    Map<String, RecordSet> existing = collectRecords(rootDomain);
    log.info("Find {} TXT records for {}", existing.size(), rootDomain);

    List<Change> changes = makeDeletionChanges(new HashMap<>(), existing);

    String comment = String.format("delete entree of %s", rootDomain);
    submitChanges(changes, comment);
    return true;
  }

  // collects all TXT records below the given name. it also update lastSeq
  @Override
  public Map<String, RecordSet> collectRecords(String rootDomain) throws Exception {
    Map<String, RecordSet> existing = new HashMap<>();
    ListResourceRecordSetsRequest.Builder request = ListResourceRecordSetsRequest.builder();
    request.hostedZoneId(zoneId);
    int page = 0;

    String rootContent = null;
    Set<DnsNode> collectServerNodes = new HashSet<>();
    while (true) {
      log.info("Loading existing TXT records from name:{} zoneId:{} page:{}", rootDomain, zoneId,
          page);
      ListResourceRecordSetsResponse response = route53Client.listResourceRecordSets(
          request.build());

      List<ResourceRecordSet> recordSetList = response.resourceRecordSets();
      for (ResourceRecordSet resourceRecordSet : recordSetList) {
        if (!isSubdomain(resourceRecordSet.name(), rootDomain)
            || resourceRecordSet.type() != RRType.TXT) {
          continue;
        }
        List<String> values = new ArrayList<>();
        for (ResourceRecord resourceRecord : resourceRecordSet.resourceRecords()) {
          values.add(resourceRecord.value());
        }
        RecordSet recordSet = new RecordSet(values.toArray(new String[0]),
            resourceRecordSet.ttl());
        String name = StringUtils.stripEnd(resourceRecordSet.name(), postfix);
        existing.put(name, recordSet);

        String content = StringUtils.join(values, "");
        content = StringUtils.strip(content, symbol);
        if (rootDomain.equalsIgnoreCase(name)) {
          rootContent = content;
        }
        if (content.startsWith(org.tron.p2p.dns.tree.Entry.nodesPrefix)) {
          NodesEntry nodesEntry;
          try {
            nodesEntry = NodesEntry.parseEntry(content);
            List<DnsNode> dnsNodes = nodesEntry.getNodes();
            collectServerNodes.addAll(dnsNodes);
          } catch (DnsException e) {
            //ignore
            log.error("Parse nodeEntry failed: {}", e.getMessage());
          }
        }
        log.info("Find name: {}", name);
      }

      if (Boolean.FALSE.equals(response.isTruncated())) {
        break;
      }
      // Set the cursor to the next batch. From the AWS docs:
      //
      // To display the next page of results, get the values of NextRecordName,
      // NextRecordType, and NextRecordIdentifier (if any) from the response. Then submit
      // another ListResourceRecordSets request, and specify those values for
      // StartRecordName, StartRecordType, and StartRecordIdentifier.
      request.startRecordIdentifier(response.nextRecordIdentifier());
      request.startRecordName(response.nextRecordName());
      request.startRecordType(response.nextRecordType());
      page += 1;
    }

    if (rootContent != null) {
      RootEntry rootEntry = RootEntry.parseEntry(rootContent);
      this.lastSeq = rootEntry.getSeq();
    }
    this.serverNodes = collectServerNodes;
    return existing;
  }

  // submits the given DNS changes to Route53.
  public void submitChanges(List<Change> changes, String comment) {
    if (changes.isEmpty()) {
      log.info("No DNS changes needed");
      return;
    }

    List<List<Change>> batchChanges = splitChanges(changes, route53ChangeSizeLimit,
        route53ChangeCountLimit);

    ChangeResourceRecordSetsResponse[] responses = new ChangeResourceRecordSetsResponse[batchChanges.size()];
    for (int i = 0; i < batchChanges.size(); i++) {
      log.info("Submit {}/{} changes to Route53", i + 1, batchChanges.size());

      ChangeBatch.Builder builder = ChangeBatch.builder();
      builder.changes(batchChanges.get(i));
      builder.comment(comment + String.format(" (%d/%d)", i + 1, batchChanges.size()));

      ChangeResourceRecordSetsRequest.Builder request = ChangeResourceRecordSetsRequest.builder();
      request.changeBatch(builder.build());
      request.hostedZoneId(this.zoneId);

      responses[i] = route53Client.changeResourceRecordSets(request.build());
    }

    // Wait for all change batches to propagate.
    for (ChangeResourceRecordSetsResponse response : responses) {
      log.info("Waiting for change request {}", response.changeInfo().id());

      GetChangeRequest.Builder request = GetChangeRequest.builder();
      request.id(response.changeInfo().id());

      int count = 0;
      while (true) {
        GetChangeResponse changeResponse = route53Client.getChange(request.build());
        count += 1;
        if (changeResponse.changeInfo().status() == ChangeStatus.INSYNC || count >= maxRetryLimit) {
          break;
        }
        try {
          Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
        }
      }
    }
    log.info("Submit {} changes complete", changes.size());
  }

  // computeChanges creates DNS changes for the given set of DNS discovery records.
  // records is the latest records to be put in Route53.
  // The 'existing' arg is the set of records that already exist on Route53.
  public List<Change> computeChanges(String domain, Map<String, String> records,
      Map<String, RecordSet> existing) {

    List<Change> changes = new ArrayList<>();
    for (Entry<String, String> entry : records.entrySet()) {
      String path = entry.getKey();
      String value = entry.getValue();
      String newValue = splitTxt(value);

      // name's ttl in our domain will not changed,
      // but this ttl on public dns server will decrease with time after request it first time
      long ttl = path.equalsIgnoreCase(domain) ? rootTTL : treeNodeTTL;

      if (!existing.containsKey(path)) {
        log.info("Create {} = {}", path, value);
        Change change = newTXTChange(ChangeAction.CREATE, path, ttl, newValue);
        changes.add(change);
      } else {
        RecordSet recordSet = existing.get(path);
        String preValue = StringUtils.join(recordSet.values, "");

        if (!preValue.equalsIgnoreCase(newValue) || recordSet.ttl != ttl) {
          log.info("Updating {} from [{}] to [{}]", path, preValue, newValue);
          if (path.equalsIgnoreCase(domain)) {
            try {
              RootEntry oldRoot = RootEntry.parseEntry(StringUtils.strip(preValue, symbol));
              RootEntry newRoot = RootEntry.parseEntry(StringUtils.strip(newValue, symbol));
              log.info("Updating root from [{}] to [{}]", oldRoot.getDnsRoot(),
                  newRoot.getDnsRoot());
            } catch (DnsException e) {
              //ignore
            }
          }
          Change change = newTXTChange(ChangeAction.UPSERT, path, ttl, newValue);
          changes.add(change);
        }
      }
    }

    List<Change> deleteChanges = makeDeletionChanges(records, existing);
    changes.addAll(deleteChanges);

    sortChanges(changes);
    return changes;
  }

  // creates record changes which delete all records not contained in 'keep'
  public List<Change> makeDeletionChanges(Map<String, String> keeps,
      Map<String, RecordSet> existing) {
    List<Change> changes = new ArrayList<>();
    for (Entry<String, RecordSet> entry : existing.entrySet()) {
      String path = entry.getKey();
      RecordSet recordSet = entry.getValue();
      if (!keeps.containsKey(path)) {
        log.info("Delete {} = {}", path, StringUtils.join(existing.get(path).values, ""));
        Change change = newTXTChange(ChangeAction.DELETE, path, recordSet.ttl, recordSet.values);
        changes.add(change);
      }
    }
    return changes;
  }

  // ensures DNS changes are in leaf-added -> root-changed -> leaf-deleted order.
  public static void sortChanges(List<Change> changes) {
    changes.sort((o1, o2) -> {
      if (getChangeOrder(o1) == getChangeOrder(o2)) {
        return o1.resourceRecordSet().name().compareTo(o2.resourceRecordSet().name());
      } else {
        return getChangeOrder(o1) - getChangeOrder(o2);
      }
    });
  }

  private static int getChangeOrder(Change change) {
    switch (change.action()) {
      case CREATE:
        return 1;
      case UPSERT:
        return 2;
      case DELETE:
        return 3;
      default:
        return 4;
    }
  }

  //  splits up DNS changes such that each change batch is smaller than the given RDATA limit.
  private static List<List<Change>> splitChanges(List<Change> changes, int sizeLimit,
      int countLimit) {
    List<List<Change>> batchChanges = new ArrayList<>();

    List<Change> subChanges = new ArrayList<>();
    int batchSize = 0;
    int batchCount = 0;
    for (Change change : changes) {
      int changeCount = getChangeCount(change);
      int changeSize = getChangeSize(change) * changeCount;

      if (batchCount + changeCount <= countLimit
          && batchSize + changeSize <= sizeLimit) {
        subChanges.add(change);
        batchCount += changeCount;
        batchSize += changeSize;
      } else {
        batchChanges.add(subChanges);
        subChanges = new ArrayList<>();
        subChanges.add(change);
        batchSize = changeSize;
        batchCount = changeCount;
      }
    }
    if (!subChanges.isEmpty()) {
      batchChanges.add(subChanges);
    }
    return batchChanges;
  }

  // returns the RDATA size of a DNS change.
  private static int getChangeSize(Change change) {
    int dataSize = 0;
    for (ResourceRecord resourceRecord : change.resourceRecordSet().resourceRecords()) {
      dataSize += resourceRecord.value().length();
    }
    return dataSize;
  }

  private static int getChangeCount(Change change) {
    if (change.action() == ChangeAction.UPSERT) {
      return 2;
    }
    return 1;
  }

  public static boolean isSameChange(Change c1, Change c2) {
    boolean isSame = c1.action().equals(c2.action())
        && c1.resourceRecordSet().ttl().longValue() == c2.resourceRecordSet().ttl().longValue()
        && c1.resourceRecordSet().name().equals(c2.resourceRecordSet().name())
        && c1.resourceRecordSet().resourceRecords().size() == c2.resourceRecordSet()
        .resourceRecords().size();
    if (!isSame) {
      return false;
    }
    List<ResourceRecord> list1 = c1.resourceRecordSet().resourceRecords();
    List<ResourceRecord> list2 = c2.resourceRecordSet().resourceRecords();
    for (int i = 0; i < list1.size(); i++) {
      if (!list1.get(i).equalsBySdkFields(list2.get(i))) {
        return false;
      }
    }
    return true;
  }

  // creates a change to a TXT record.
  public Change newTXTChange(ChangeAction action, String key, long ttl, String... values) {
    ResourceRecordSet.Builder builder = ResourceRecordSet.builder()
        .name(key)
        .type(RRType.TXT)
        .ttl(ttl);
    List<ResourceRecord> resourceRecords = new ArrayList<>();
    for (String value : values) {
      ResourceRecord.Builder builder1 = ResourceRecord.builder();
      builder1.value(value);
      resourceRecords.add(builder1.build());
    }
    builder.resourceRecords(resourceRecords);

    Change.Builder builder2 = Change.builder();
    builder2.action(action);
    builder2.resourceRecordSet(builder.build());
    return builder2.build();
  }

  // splits value into a list of quoted 255-character strings.
  // only used in CREATE and UPSERT
  private String splitTxt(String value) {
    StringBuilder sb = new StringBuilder();
    while (value.length() > 253) {
      sb.append(symbol).append(value, 0, 253).append(symbol);
      value = value.substring(253);
    }
    if (value.length() > 0) {
      sb.append(symbol).append(value).append(symbol);
    }
    return sb.toString();
  }

  public static boolean isSubdomain(String sub, String root) {
    String subNoSuffix = postfix + StringUtils.strip(sub, postfix);
    String rootNoSuffix = postfix + StringUtils.strip(root, postfix);
    return subNoSuffix.endsWith(rootNoSuffix);
  }

  public static class RecordSet {

    String[] values;
    long ttl;

    public RecordSet(String[] values, long ttl) {
      this.values = values;
      this.ttl = ttl;
    }
  }
}
