package org.tron.p2p.dns.tree;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;
import org.tron.p2p.utils.ByteArray;

@Slf4j(topic = "net")
public class LinkEntry implements Entry {

  @Getter
  private final String represent;
  @Getter
  private final String domain;
  @Getter
  private final String unCompressHexPublicKey;

  public LinkEntry(String represent, String domain, String unCompressHexPublicKey) {
    this.represent = represent;
    this.domain = domain;
    this.unCompressHexPublicKey = unCompressHexPublicKey;
  }

  public static LinkEntry parseEntry(String treeRepresent) throws DnsException {
    if (!treeRepresent.startsWith(linkPrefix)) {
      throw new DnsException(TypeEnum.INVALID_SCHEME_URL,
          "scheme url must starts with :[" + Entry.linkPrefix + "], but get " + treeRepresent);
    }
    String[] items = treeRepresent.substring(linkPrefix.length()).split("@");
    if (items.length != 2) {
      throw new DnsException(TypeEnum.NO_PUBLIC_KEY, "scheme url:" + treeRepresent);
    }
    String base32PublicKey = items[0];

    try {
      byte[] data = Algorithm.decode32(base32PublicKey);
      String unCompressPublicKey = Algorithm.decompressPubKey(ByteArray.toHexString(data));
      return new LinkEntry(treeRepresent, items[1], unCompressPublicKey);
    } catch (RuntimeException exception) {
      throw new DnsException(TypeEnum.BAD_PUBLIC_KEY, "bad public key:" + base32PublicKey);
    }
  }

  public static String buildRepresent(String base32PubKey, String domain) {
    return linkPrefix + base32PubKey + "@" + domain;
  }

  @Override
  public String toString() {
    return represent;
  }
}
