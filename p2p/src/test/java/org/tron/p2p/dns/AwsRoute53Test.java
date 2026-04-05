package org.tron.p2p.dns;


import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.dns.update.AwsClient;
import org.tron.p2p.dns.update.AwsClient.RecordSet;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.exception.DnsException;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;

public class AwsRoute53Test {

  @Test
  public void testChangeSort() {

    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("n", new RecordSet(new String[] {
        "tree-root-v1:CjoKGlVKQU9JQlMyUFlZMjJYUU1WRlNXT1RZSlhVEhpGRFhOM1NONjdOQTVES0E0SjJHT0s3QlZRSRgIEldBTE5aWHEyRkk5Ui1ubjdHQk9HdWJBRFVPakZ2MWp5TjZiUHJtSWNTNks0ZE0wc1dKMUwzT2paWFRGei1KcldDenZZVHJId2RMSTlUczRPZ2Q4TXlJUnM"},
        AwsClient.rootTTL));
    existing.put("2kfjogvxdqtxxugbh7gs7naaai.n", new RecordSet(new String[] {
        "nodes:-HW4QO1ml1DdXLeZLsUxewnthhUy8eROqkDyoMTyavfks9JlYQIlMFEUoM78PovJDPQrAkrb3LRJ-",
        "vtrymDguKCOIAWAgmlkgnY0iXNlY3AyNTZrMaEDffaGfJzgGhUif1JqFruZlYmA31HzathLSWxfbq_QoQ4"},
        3333));
    existing.put("fdxn3sn67na5dka4j2gok7bvqi.n",
        new RecordSet(new String[] {"tree-branch:"}, AwsClient.treeNodeTTL));

    Map<String, String> newRecords = new HashMap<>();
    newRecords.put("n",
        "tree-root-v1:CjoKGkZEWE4zU042N05BNURLQTRKMkdPSzdCVlFJEhpGRFhOM1NONjdOQTVES0E0SjJHT0s3QlZRSRgJElc5aDU4d1cyajUzdlBMeHNBSGN1cDMtV0ZEM2lvZUk4SkJrZkdYSk93dmI0R0lHR01pQVAxRkJVVGc4bHlORERleXJkck9uSDdSbUNUUnJRVGxqUm9UaHM");
    newRecords.put("c7hrfpf3blgf3yr4dy5kx3smbe.n",
        "tree://AM5FCQLWIZX2QFPNJAP7VUERCCRNGRHWZG3YYHIUV7BVDQ5FDPRT2@morenodes.example.org");
    newRecords.put("jwxydbpxywg6fx3gmdibfa6cj4.n",
        "tree-branch:2XS2367YHAXJFGLZHVAWLQD4ZY,H4FHT4B454P6UXFD7JCYQ5PWDY,MHTDO6TMUBRIA2XWG5LUDACK24");
    newRecords.put("2xs2367yhaxjfglzhvawlqd4zy.n",
        "nodes:-HW4QOFzoVLaFJnNhbgMoDXPnOvcdVuj7pDpqRvh6BRDO68aVi5ZcjB3vzQRZH2IcLBGHzo8uUN3snqmgTiE56CH3AMBgmlkgnY0iXNlY3AyNTZrMaECC2_24YYkYHEgdzxlSNKQEnHhuNAbNlMlWJxrJxbAFvA");
    newRecords.put("h4fht4b454p6uxfd7jcyq5pwdy.n",
        "nodes:-HW4QAggRauloj2SDLtIHN1XBkvhFZ1vtf1raYQp9TBW2RD5EEawDzbtSmlXUfnaHcvwOizhVYLtr7e6vw7NAf6mTuoCgmlkgnY0iXNlY3AyNTZrMaECjrXI8TLNXU0f8cthpAMxEshUyQlK-AM0PW2wfrnacNI");
    newRecords.put("mhtdo6tmubria2xwg5ludack24.n",
        "nodes:-HW4QLAYqmrwllBEnzWWs7I5Ev2IAs7x_dZlbYdRdMUx5EyKHDXp7AV5CkuPGUPdvbv1_Ms1CPfhcGCvSElSosZmyoqAgmlkgnY0iXNlY3AyNTZrMaECriawHKWdDRk2xeZkrOXBQ0dfMFLHY4eENZwdufn1S1o");

    AwsClient publish;
    try {
      publish = new AwsClient("random1", "random2", "random3",
          "us-east-1", new P2pConfig().getPublishConfig().getChangeThreshold());
    } catch (DnsException e) {
      Assert.fail();
      return;
    }
    List<Change> changes = publish.computeChanges("n", newRecords, existing);

    Change[] wantChanges = new Change[] {
        publish.newTXTChange(ChangeAction.CREATE, "2xs2367yhaxjfglzhvawlqd4zy.n",
            AwsClient.treeNodeTTL,
            "\"nodes:-HW4QOFzoVLaFJnNhbgMoDXPnOvcdVuj7pDpqRvh6BRDO68aVi5ZcjB3vzQRZH2IcLBGHzo8uUN3snqmgTiE56CH3AMBgmlkgnY0iXNlY3AyNTZrMaECC2_24YYkYHEgdzxlSNKQEnHhuNAbNlMlWJxrJxbAFvA\""),
        publish.newTXTChange(ChangeAction.CREATE, "c7hrfpf3blgf3yr4dy5kx3smbe.n",
            AwsClient.treeNodeTTL,
            "\"tree://AM5FCQLWIZX2QFPNJAP7VUERCCRNGRHWZG3YYHIUV7BVDQ5FDPRT2@morenodes.example.org\""),
        publish.newTXTChange(ChangeAction.CREATE, "h4fht4b454p6uxfd7jcyq5pwdy.n",
            AwsClient.treeNodeTTL,
            "\"nodes:-HW4QAggRauloj2SDLtIHN1XBkvhFZ1vtf1raYQp9TBW2RD5EEawDzbtSmlXUfnaHcvwOizhVYLtr7e6vw7NAf6mTuoCgmlkgnY0iXNlY3AyNTZrMaECjrXI8TLNXU0f8cthpAMxEshUyQlK-AM0PW2wfrnacNI\""),
        publish.newTXTChange(ChangeAction.CREATE, "jwxydbpxywg6fx3gmdibfa6cj4.n",
            AwsClient.treeNodeTTL,
            "\"tree-branch:2XS2367YHAXJFGLZHVAWLQD4ZY,H4FHT4B454P6UXFD7JCYQ5PWDY,MHTDO6TMUBRIA2XWG5LUDACK24\""),
        publish.newTXTChange(ChangeAction.CREATE, "mhtdo6tmubria2xwg5ludack24.n",
            AwsClient.treeNodeTTL,
            "\"nodes:-HW4QLAYqmrwllBEnzWWs7I5Ev2IAs7x_dZlbYdRdMUx5EyKHDXp7AV5CkuPGUPdvbv1_Ms1CPfhcGCvSElSosZmyoqAgmlkgnY0iXNlY3AyNTZrMaECriawHKWdDRk2xeZkrOXBQ0dfMFLHY4eENZwdufn1S1o\""),

        publish.newTXTChange(ChangeAction.UPSERT, "n",
            AwsClient.rootTTL,
            "\"tree-root-v1:CjoKGkZEWE4zU042N05BNURLQTRKMkdPSzdCVlFJEhpGRFhOM1NONjdOQTVES0E0SjJHT0s3QlZRSRgJElc5aDU4d1cyajUzdlBMeHNBSGN1cDMtV0ZEM2lvZUk4SkJrZkdYSk93dmI0R0lHR01pQVAxRkJVVGc4bHlORERleXJkck9uSDdSbUNUUnJRVGxqUm9UaHM\""),

        publish.newTXTChange(ChangeAction.DELETE, "2kfjogvxdqtxxugbh7gs7naaai.n",
            3333,
            "nodes:-HW4QO1ml1DdXLeZLsUxewnthhUy8eROqkDyoMTyavfks9JlYQIlMFEUoM78PovJDPQrAkrb3LRJ-",
            "vtrymDguKCOIAWAgmlkgnY0iXNlY3AyNTZrMaEDffaGfJzgGhUif1JqFruZlYmA31HzathLSWxfbq_QoQ4"),
        publish.newTXTChange(ChangeAction.DELETE, "fdxn3sn67na5dka4j2gok7bvqi.n",
            AwsClient.treeNodeTTL,
            "tree-branch:")
    };

    Assert.assertEquals(wantChanges.length, changes.size());
    for (int i = 0; i < changes.size(); i++) {
      Assert.assertTrue(wantChanges[i].equalsBySdkFields(changes.get(i)));
      Assert.assertTrue(AwsClient.isSameChange(wantChanges[i], changes.get(i)));
    }
  }

  @Test
  public void testPublish() throws UnknownHostException {

    DnsNode[] nodes = TreeTest.sampleNode();
    List<DnsNode> nodeList = Arrays.asList(nodes);
    List<String> enrList = Tree.merge(nodeList, new PublishConfig().getMaxMergeSize());

    String[] links = new String[] {
        "tree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@example1.org",
        "tree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@example2.org"};
    List<String> linkList = Arrays.asList(links);

    Tree tree = new Tree();
    try {
      tree.makeTree(1, enrList, linkList, AlgorithmTest.privateKey);
    } catch (DnsException e) {
      Assert.fail();
    }

//    //warning: replace your key in the following section, or this test will fail
//    AwsClient awsClient;
//    try {
//      awsClient = new AwsClient("replace your access key",
//          "replace your access key secret",
//          "replace your host zone id",
//          Region.US_EAST_1);
//    } catch (DnsException e) {
//      Assert.fail();
//      return;
//    }
//    String domain = "replace with your domain";
//    try {
//      awsClient.deploy(domain, tree);
//    } catch (Exception e) {
//      Assert.fail();
//      return;
//    }
//
//    BigInteger publicKeyInt = Algorithm.generateKeyPair(AlgorithmTest.privateKey).getPublicKey();
//    String puKeyCompress = Algorithm.compressPubKey(publicKeyInt);
//    String base32Pubkey = Algorithm.encode32(ByteArray.fromHexString(puKeyCompress));
//    Client client = new Client();
//
//    Tree route53Tree = new Tree();
//    try {
//      client.syncTree(Entry.linkPrefix + base32Pubkey + "@" + domain, null,
//          route53Tree);
//    } catch (Exception e) {
//      Assert.fail();
//      return;
//    }
//    Assert.assertEquals(links.length, route53Tree.getLinksEntry().size());
//    Assert.assertEquals(nodes.length, route53Tree.getDnsNodes().size());
  }
}
