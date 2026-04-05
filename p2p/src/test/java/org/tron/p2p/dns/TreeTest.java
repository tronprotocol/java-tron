package org.tron.p2p.dns;


import com.google.protobuf.InvalidProtocolBufferException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.exception.DnsException;

public class TreeTest {

  public static DnsNode[] sampleNode() throws UnknownHostException {
    return new DnsNode[] {
        new DnsNode(null, "192.168.0.1", null, 10000),
        new DnsNode(null, "192.168.0.2", null, 10000),
        new DnsNode(null, "192.168.0.3", null, 10000),
        new DnsNode(null, "192.168.0.4", null, 10000),
        new DnsNode(null, "192.168.0.5", null, 10000),
        new DnsNode(null, "192.168.0.6", null, 10001),
        new DnsNode(null, "192.168.0.6", null, 10002),
        new DnsNode(null, "192.168.0.6", null, 10003),
        new DnsNode(null, "192.168.0.6", null, 10004),
        new DnsNode(null, "192.168.0.6", null, 10005),
        new DnsNode(null, "192.168.0.10", "fe80::0001", 10005),
        new DnsNode(null, "192.168.0.10", "fe80::0002", 10005),
        new DnsNode(null, null, "fe80::0001", 10000),
        new DnsNode(null, null, "fe80::0002", 10000),
        new DnsNode(null, null, "fe80::0003", 10001),
        new DnsNode(null, null, "fe80::0004", 10001),
    };
  }

  @Test
  public void testMerge() throws UnknownHostException {
    DnsNode[] nodes = sampleNode();
    List<DnsNode> nodeList = Arrays.asList(nodes);

    int maxMergeSize = new PublishConfig().getMaxMergeSize();
    List<String> enrs = Tree.merge(nodeList, maxMergeSize);
    int total = 0;
    for (int i = 0; i < enrs.size(); i++) {
      List<DnsNode> subList = null;
      try {
        subList = DnsNode.decompress(enrs.get(i).substring(Entry.nodesPrefix.length()));
      } catch (InvalidProtocolBufferException e) {
        Assert.fail();
      }
      Assert.assertTrue(subList.size() <= maxMergeSize);
      total += subList.size();
    }
    Assert.assertEquals(nodeList.size(), total);
  }

  @Test
  public void testTreeBuild() throws UnknownHostException {
    int seq = 0;

    DnsNode[] dnsNodes = new DnsNode[] {
        new DnsNode(null, "192.168.0.1", null, 10000),
        new DnsNode(null, "192.168.0.2", null, 10000),
        new DnsNode(null, "192.168.0.3", null, 10000),
        new DnsNode(null, "192.168.0.4", null, 10000),
        new DnsNode(null, "192.168.0.5", null, 10000),
        new DnsNode(null, "192.168.0.6", null, 10000),
        new DnsNode(null, "192.168.0.7", null, 10000),
        new DnsNode(null, "192.168.0.8", null, 10000),
        new DnsNode(null, "192.168.0.9", null, 10000),
        new DnsNode(null, "192.168.0.10", null, 10000),

        new DnsNode(null, "192.168.0.11", null, 10000),
        new DnsNode(null, "192.168.0.12", null, 10000),
        new DnsNode(null, "192.168.0.13", null, 10000),
        new DnsNode(null, "192.168.0.14", null, 10000),
        new DnsNode(null, "192.168.0.15", null, 10000),
        new DnsNode(null, "192.168.0.16", null, 10000),
        new DnsNode(null, "192.168.0.17", null, 10000),
        new DnsNode(null, "192.168.0.18", null, 10000),
        new DnsNode(null, "192.168.0.19", null, 10000),
        new DnsNode(null, "192.168.0.20", null, 10000),

        new DnsNode(null, "192.168.0.21", null, 10000),
        new DnsNode(null, "192.168.0.22", null, 10000),
        new DnsNode(null, "192.168.0.23", null, 10000),
        new DnsNode(null, "192.168.0.24", null, 10000),
        new DnsNode(null, "192.168.0.25", null, 10000),
        new DnsNode(null, "192.168.0.26", null, 10000),
        new DnsNode(null, "192.168.0.27", null, 10000),
        new DnsNode(null, "192.168.0.28", null, 10000),
        new DnsNode(null, "192.168.0.29", null, 10000),
        new DnsNode(null, "192.168.0.30", null, 10000),

        new DnsNode(null, "192.168.0.31", null, 10000),
        new DnsNode(null, "192.168.0.32", null, 10000),
        new DnsNode(null, "192.168.0.33", null, 10000),
        new DnsNode(null, "192.168.0.34", null, 10000),
        new DnsNode(null, "192.168.0.35", null, 10000),
        new DnsNode(null, "192.168.0.36", null, 10000),
        new DnsNode(null, "192.168.0.37", null, 10000),
        new DnsNode(null, "192.168.0.38", null, 10000),
        new DnsNode(null, "192.168.0.39", null, 10000),
        new DnsNode(null, "192.168.0.40", null, 10000),
    };

    String[] enrs = new String[dnsNodes.length];
    for (int i = 0; i < dnsNodes.length; i++) {
      DnsNode dnsNode = dnsNodes[i];
      List<DnsNode> nodeList = new ArrayList<>();
      nodeList.add(dnsNode);
      enrs[i] = Entry.nodesPrefix + DnsNode.compress(nodeList);
    }

    String[] links = new String[] {};

    String linkBranch0 = "tree-branch:";
    String enrBranch1 = "tree-branch:OX22LN2ZUGOPGIPGBUQH35KZU4,XTGCXXQHPK3VUZPQHC6CGJDR3Q,BQLJLB6P5CRXHI37BRVWBWWACY,X4FURUK4SHXW3GVE6XBO3DFD5Y,SIUYMSVBYYXCE6HVW5TSGOFKVQ,2RKY3FUYIQBV4TFIDU7S42EIEU,KSEEGRTUGR4GCCBQ4TYHAWDKME,YGWDS6F6KLTFCC7T3AMAJHXI2A,K4HMVDEHRKOGOFQZXBJ2PSVIMM,NLLRMPWOTS6SP4D7YLCQA42IQQ,BBDLEDOZYAX5CWM6GNAALRVUXY,7NMT4ZISY5F4U6B6CQML2C526E,NVDRYMFHIERJEVGW5TE7QEAS2A";
    String enrBranch2 = "tree-branch:5ELKMY4HVAV5CBY6KDMXWOFSN4,7PHYT72EXSZJ6MT2IQ7VGUFQHI,AM6BJFCERRNKBG4A5X3MORBDZU,2WOYKPVTNYAY3KVDTDY4CEVOJM,PW5BHSJMPEHVJKRF5QTRXQB4LU,IS4YMOJGD4XPODBAMHZOUTIVMI,NSEE5WE57FWG2EERXI5TBBD32E,GOLZDJTTQ7V2MO2BG45O3Q22XI,4VL7USGBWKW576WM4TX7XIXS4A,GZQSPHDZYS7FXURGOQU3RIDUK4,T7L645CJJKCQVQMUADDO44EGOM,ATPMZZZB4RGYKC6K7QDFC22WIE,57KNNYA4WOKVZAODRCFYK64MBA";
    String enrBranch3 = "tree-branch:BJF5S37KVATG2SYHO6M7APDCNU,OUB3BDKUZQWXXFX5OSF5JCB6BA,6JZEHDWM6WWQYIEYVZN5QVMUXA,LXNNOBVTTZBPD3N5VTOCPVG7JE,LMWLKDCBT2U3CGSHKR2PYJNV5I,K2SSCP4ZIF7TQI4MRVLELFAQQE,MKR7II3GYETKN7MSCUQOF6MBQ4,FBJ5VFCV37SGUOEYA2SPGO3TLA,6SHSDL7PJCJAER3OS53NYPNDFI,KYU2OQJBU6AU3KJFCUSLOJWKVE,3N6XKDWY3WTBOSBS22YPUAHCFQ,IPEWOISXUGOL7ORZIOXBD24SPI,PCGDGGVEQQQFL4U2FYRXVHVMUM";
    String enrBranch4 = "tree-branch:WHCXLEQB3467BFATRY5SMIV62M,LAHEXJDXOPZSS2TDVXTJACCB6Q,QR4HMFZU3STBJEXOZIXPDRQTGM,JZUKVXBOLBPXCELWIE5G6E6UUU";

    String[] branches = new String[] {linkBranch0, enrBranch1, enrBranch2, enrBranch3, enrBranch4};

    List<String> branchList = Arrays.asList(branches);
    List<String> enrList = Arrays.asList(enrs);
    List<String> linkList = Arrays.asList(links);

    Tree tree = new Tree();
    try {
      tree.makeTree(seq, enrList, linkList, null);
    } catch (DnsException e) {
      Assert.fail();
    }

    /*
                                  b  r  a  n  c  h  4
                   /                 /           \          \
                /                  /               \           \
             /                    /                   \            \
          branch1               branch2              branch3          \
        /      \              /       \             /       \           \
      node:-01 ~ node:-13  node:-14 ~ node:-26   node:-27 ~ node:-39  node:-40
    */

    Assert.assertEquals(branchList.size() + enrList.size() + linkList.size(),
        tree.getEntries().size());
    Assert.assertEquals(branchList.size(), tree.getBranchesEntry().size());
    Assert.assertEquals(enrList.size(), tree.getNodesEntry().size());
    Assert.assertEquals(linkList.size(), tree.getLinksEntry().size());

    for (String branch : tree.getBranchesEntry()) {
      Assert.assertTrue(branchList.contains(branch));
    }
    for (String nodeEntry : tree.getNodesEntry()) {
      Assert.assertTrue(enrList.contains(nodeEntry));
    }
    for (String link : tree.getLinksEntry()) {
      Assert.assertTrue(linkList.contains(link));
    }

    Assert.assertEquals(Algorithm.encode32AndTruncate(enrBranch4), tree.getRootEntry().getERoot());
    Assert.assertEquals(Algorithm.encode32AndTruncate(linkBranch0), tree.getRootEntry().getLRoot());
    Assert.assertEquals(seq, tree.getSeq());
  }

  @Test
  public void testGroupAndMerge() throws UnknownHostException {
    Random random = new Random();
    //simulate some nodes
    int ipCount = 2000;
    int maxMergeSize = 5;
    List<DnsNode> dnsNodes = new ArrayList<>();
    Set<String> ipSet = new HashSet<>();
    int i = 0;
    while (i < ipCount) {
      i += 1;
      String ip = String.format("%d.%d.%d.%d", random.nextInt(256), random.nextInt(256),
          random.nextInt(256), random.nextInt(256));
      if (ipSet.contains(ip)) {
        continue;
      }
      ipSet.add(ip);
      dnsNodes.add(new DnsNode(null, ip, null, 10000));
    }
    Set<String> enrSet1 = new HashSet<>(Tree.merge(dnsNodes, maxMergeSize));
    System.out.println("srcSize:" + enrSet1.size());

    // delete some node
    int deleteCount = 100;
    i = 0;
    while (i < deleteCount) {
      i += 1;
      int deleteIndex = random.nextInt(dnsNodes.size());
      dnsNodes.remove(deleteIndex);
    }

    // add some node
    int addCount = 100;
    i = 0;
    while (i < addCount) {
      i += 1;
      String ip = String.format("%d.%d.%d.%d", random.nextInt(256), random.nextInt(256),
          random.nextInt(256), random.nextInt(256));
      if (ipSet.contains(ip)) {
        continue;
      }
      ipSet.add(ip);
      dnsNodes.add(new DnsNode(null, ip, null, 10000));
    }
    Set<String> enrSet2 = new HashSet<>(Tree.merge(dnsNodes, maxMergeSize));

    // calculate changes
    Set<String> enrSet3 = new HashSet<>(enrSet2);
    enrSet3.removeAll(enrSet1); // enrSet2 - enrSet1
    System.out.println("addSize:" + enrSet3.size());
    Assert.assertTrue(enrSet3.size() < enrSet1.size());

    Set<String> enrSet4 = new HashSet<>(enrSet1);
    enrSet4.removeAll(enrSet2); //enrSet1 - enrSet2
    System.out.println("deleteSize:" + enrSet4.size());
    Assert.assertTrue(enrSet4.size() < enrSet1.size());

    Set<String> enrSet5 = new HashSet<>(enrSet1);
    enrSet5.retainAll(enrSet2); // enrSet1 && enrSet2
    System.out.println("intersectionSize:" + enrSet5.size());
    Assert.assertTrue(enrSet5.size() < enrSet1.size());
  }
}
