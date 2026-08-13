package org.tron.p2p.dns.update;


import java.util.Map;
import org.tron.p2p.dns.tree.Tree;

public interface Publish<T> {

  int rootTTL = 10 * 60;
  int treeNodeTTL = 7 * 24 * 60 * 60;

  void testConnect() throws Exception;

  void deploy(String domainName, Tree t) throws Exception;

  boolean deleteDomain(String domainName) throws Exception;

  Map<String, T> collectRecords(String domainName) throws Exception;
}
