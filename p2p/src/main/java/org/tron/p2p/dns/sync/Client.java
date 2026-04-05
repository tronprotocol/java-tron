package org.tron.p2p.dns.sync;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.UnknownHostException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.dns.lookup.LookUpTxt;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.dns.tree.BranchEntry;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.LinkEntry;
import org.tron.p2p.dns.tree.NodesEntry;
import org.tron.p2p.dns.tree.RootEntry;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;
import org.tron.p2p.utils.ByteArray;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;

@Slf4j(topic = "net")
public class Client {

  public static final int recheckInterval = 60 * 60; //seconds, should be smaller than rootTTL
  public static final int cacheLimit = 2000;
  public static final int randomRetryTimes = 10;
  private Cache<String, Entry> cache;
  @Getter
  private final Map<String, Tree> trees = new ConcurrentHashMap<>();
  private final Map<String, ClientTree> clientTrees = new HashMap<>();

  private final ScheduledExecutorService syncer = Executors.newSingleThreadScheduledExecutor(
      BasicThreadFactory.builder().namingPattern("dnsSyncer").build());

  public Client() {
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(cacheLimit)
        .recordStats()
        .build();
  }

  public void init() {
    if (!Parameter.p2pConfig.getTreeUrls().isEmpty()) {
      syncer.scheduleWithFixedDelay(this::startSync, 5, recheckInterval,
          TimeUnit.SECONDS);
    }
  }

  public void startSync() {
    for (String urlScheme : Parameter.p2pConfig.getTreeUrls()) {
      ClientTree clientTree = clientTrees.getOrDefault(urlScheme, new ClientTree(this));
      Tree tree = trees.getOrDefault(urlScheme, new Tree());
      trees.put(urlScheme, tree);
      clientTrees.put(urlScheme, clientTree);
      try {
        syncTree(urlScheme, clientTree, tree);
      } catch (Exception e) {
        log.error("SyncTree failed, url:" + urlScheme, e);
        continue;
      }
    }
  }

  public void syncTree(String urlScheme, ClientTree clientTree, Tree tree) throws Exception {
    LinkEntry loc = LinkEntry.parseEntry(urlScheme);
    if (clientTree == null) {
      clientTree = new ClientTree(this);
    }
    if (clientTree.getLinkEntry() == null) {
      clientTree.setLinkEntry(loc);
    }
    if (tree.getEntries().isEmpty()) {
      // when sync tree first time, we can get the entries dynamically
      clientTree.syncAll(tree.getEntries());
    } else {
      Map<String, Entry> tmpEntries = new HashMap<>();
      boolean[] isRootUpdate = clientTree.syncAll(tmpEntries);
      if (!isRootUpdate[0]) {
        tmpEntries.putAll(tree.getLinksMap());
      }
      if (!isRootUpdate[1]) {
        tmpEntries.putAll(tree.getNodesMap());
      }
      // we update the entries after sync finishes, ignore branch difference
      tree.setEntries(tmpEntries);
    }

    tree.setRootEntry(clientTree.getRoot());
    log.info("SyncTree {} complete, LinkEntry size:{}, NodesEntry size:{}, node size:{}",
        urlScheme, tree.getLinksEntry().size(), tree.getNodesEntry().size(),
        tree.getDnsNodes().size());
  }

  public RootEntry resolveRoot(LinkEntry linkEntry) throws TextParseException, DnsException,
      SignatureException, UnknownHostException {
    //do not put root in cache
    TXTRecord txtRecord = LookUpTxt.lookUpTxt(linkEntry.getDomain());
    if (txtRecord == null) {
      throw new DnsException(TypeEnum.LOOK_UP_ROOT_FAILED, "domain: " + linkEntry.getDomain());
    }
    for (String txt : txtRecord.getStrings()) {
      if (txt.startsWith(Entry.rootPrefix)) {
        return RootEntry.parseEntry(txt, linkEntry.getUnCompressHexPublicKey(),
            linkEntry.getDomain());
      }
    }
    throw new DnsException(TypeEnum.NO_ROOT_FOUND, "domain: " + linkEntry.getDomain());
  }

  // resolveEntry retrieves an entry from the cache or fetches it from the network if it isn't cached.
  public Entry resolveEntry(String domain, String hash)
      throws DnsException, TextParseException, UnknownHostException {
    Entry entry = cache.getIfPresent(hash);
    if (entry != null) {
      return entry;
    }
    entry = doResolveEntry(domain, hash);
    if (entry != null) {
      cache.put(hash, entry);
    }
    return entry;
  }

  private Entry doResolveEntry(String domain, String hash)
      throws DnsException, TextParseException, UnknownHostException {
    try {
      ByteArray.toHexString(Algorithm.decode32(hash));
    } catch (Exception e) {
      throw new DnsException(TypeEnum.OTHER_ERROR, "invalid base32 hash: " + hash);
    }
    TXTRecord txtRecord = LookUpTxt.lookUpTxt(hash, domain);
    if (txtRecord == null) {
      return null;
    }
    String txt = LookUpTxt.joinTXTRecord(txtRecord);

    Entry entry = null;
    if (txt.startsWith(Entry.branchPrefix)) {
      entry = BranchEntry.parseEntry(txt);
    } else if (txt.startsWith(Entry.linkPrefix)) {
      entry = LinkEntry.parseEntry(txt);
    } else if (txt.startsWith(Entry.nodesPrefix)) {
      entry = NodesEntry.parseEntry(txt);
    }

    if (entry == null) {
      throw new DnsException(TypeEnum.NO_ENTRY_FOUND,
          String.format("hash:%s, domain:%s, txt:%s", hash, domain, txt));
    }

    String wantHash = Algorithm.encode32AndTruncate(entry.toString());
    if (!wantHash.equals(hash)) {
      throw new DnsException(TypeEnum.HASH_MISS_MATCH,
          String.format("hash mismatch, want: [%s], really: [%s], content: [%s]", wantHash, hash,
              entry));
    }
    return entry;
  }

  public RandomIterator newIterator() {
    RandomIterator randomIterator = new RandomIterator(this);
    for (String urlScheme : Parameter.p2pConfig.getTreeUrls()) {
      try {
        randomIterator.addTree(urlScheme);
      } catch (DnsException e) {
        log.error("AddTree failed " + urlScheme, e);
      }
    }
    return randomIterator;
  }

  public void close() {
    if (syncer != null) {
      syncer.shutdown();
    }
  }
}
