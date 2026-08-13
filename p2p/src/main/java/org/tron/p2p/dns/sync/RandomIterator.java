package org.tron.p2p.dns.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.dns.tree.LinkEntry;
import org.tron.p2p.exception.DnsException;


@Slf4j(topic = "net")
public class RandomIterator implements Iterator<DnsNode> {

  private final Client client;
  private Map<String, ClientTree> clientTrees;
  @Getter
  private DnsNode cur;
  private final LinkCache linkCache;
  private final Random random;

  public RandomIterator(Client client) {
    this.client = client;
    clientTrees = new ConcurrentHashMap<>();
    linkCache = new LinkCache();
    random = new Random();
  }

  //syncs random tree entries until it finds a node.
  @Override
  public DnsNode next() {
    int i = 0;
    while (i < Client.randomRetryTimes) {
      i += 1;
      ClientTree clientTree = pickTree();
      if (clientTree == null) {
        log.error("clientTree is null");
        return null;
      }
      log.info("Choose clientTree:{} from {} ClientTree", clientTree.getLinkEntry().getRepresent(),
          clientTrees.size());
      DnsNode dnsNode;
      try {
        dnsNode = clientTree.syncRandom();
      } catch (Exception e) {
        log.warn("Error in DNS random node sync, tree:{}, cause:[{}]",
            clientTree.getLinkEntry().getDomain(), e.getMessage());
        continue;
      }
      if (dnsNode != null && dnsNode.getPreferInetSocketAddress() != null) {
        return dnsNode;
      }
    }
    return null;
  }

  @Override
  public boolean hasNext() {
    this.cur = next();
    return this.cur != null;
  }

  public void addTree(String url) throws DnsException {
    LinkEntry linkEntry = LinkEntry.parseEntry(url);
    linkCache.addLink("", linkEntry.getRepresent());
  }

  //the first random
  private ClientTree pickTree() {
    if (clientTrees == null) {
      log.info("clientTrees is null");
      return null;
    }
    if (linkCache.isChanged()) {
      rebuildTrees();
      linkCache.setChanged(false);
    }

    int size = clientTrees.size();
    List<ClientTree> allTrees = new ArrayList<>(clientTrees.values());

    return allTrees.get(random.nextInt(size));
  }

  // rebuilds the 'trees' map.
  // if urlScheme is not contain in any other link, wo delete it from clientTrees
  // then create one ClientTree using this urlScheme， add it to clientTrees
  private void rebuildTrees() {
    log.info("rebuildTrees...");
    Iterator<Entry<String, ClientTree>> it = clientTrees.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, ClientTree> entry = it.next();
      String urlScheme = entry.getKey();
      if (!linkCache.isContainInOtherLink(urlScheme)) {
        log.info("remove tree from trees:{}", urlScheme);
        it.remove();
      }
    }

    for (Entry<String, Set<String>> entry : linkCache.backrefs.entrySet()) {
      String urlScheme = entry.getKey();
      if (!clientTrees.containsKey(urlScheme)) {
        try {
          LinkEntry linkEntry = LinkEntry.parseEntry(urlScheme);
          clientTrees.put(urlScheme, new ClientTree(client, linkCache, linkEntry));
          log.info("add tree to clientTrees:{}", urlScheme);
        } catch (DnsException e) {
          log.error("Parse LinkEntry failed", e);
        }
      }
    }
    log.info("Exist clientTrees: {}", StringUtils.join(clientTrees.keySet(), ","));
  }

  public void close() {
    clientTrees = null;
  }
}
