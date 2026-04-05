package org.tron.p2p.dns.tree;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "net")
public class BranchEntry implements Entry {

  private static final String splitSymbol = ",";
  @Getter
  private String[] children;

  public BranchEntry(String[] children) {
    this.children = children;
  }

  public static BranchEntry parseEntry(String e) {
    String content = e.substring(branchPrefix.length());
    if (StringUtils.isEmpty(content)) {
      log.info("children size is 0, e:[{}]", e);
      return new BranchEntry(new String[0]);
    } else {
      return new BranchEntry(content.split(splitSymbol));
    }
  }

  @Override
  public String toString() {
    return branchPrefix + StringUtils.join(children, splitSymbol);
  }
}
