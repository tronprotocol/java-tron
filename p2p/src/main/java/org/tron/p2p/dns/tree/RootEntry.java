package org.tron.p2p.dns.tree;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;
import org.tron.p2p.protos.Discover.DnsRoot;
import org.tron.p2p.utils.ByteArray;

@Slf4j(topic = "net")
public class RootEntry implements Entry {

  @Getter
  private DnsRoot dnsRoot;

  public RootEntry(DnsRoot dnsRoot) {
    this.dnsRoot = dnsRoot;
  }

  public String getERoot() {
    return new String(dnsRoot.getTreeRoot().getERoot().toByteArray());
  }

  public String getLRoot() {
    return new String(dnsRoot.getTreeRoot().getLRoot().toByteArray());
  }

  public int getSeq() {
    return dnsRoot.getTreeRoot().getSeq();
  }

  public void setSeq(int seq) {
    DnsRoot.TreeRoot.Builder builder = dnsRoot.getTreeRoot().toBuilder();
    builder.setSeq(seq);

    DnsRoot.Builder dnsRootBuilder = dnsRoot.toBuilder();
    dnsRootBuilder.setTreeRoot(builder.build());

    this.dnsRoot = dnsRootBuilder.build();
  }

  public byte[] getSignature() {
    return Algorithm.decode64(new String(dnsRoot.getSignature().toByteArray()));
  }

  public void setSignature(byte[] signature) {
    DnsRoot.Builder dnsRootBuilder = dnsRoot.toBuilder();
    dnsRootBuilder.setSignature(ByteString.copyFrom(Algorithm.encode64(signature).getBytes()));
    this.dnsRoot = dnsRootBuilder.build();
  }

  public RootEntry(String eRoot, String lRoot, int seq) {
    DnsRoot.TreeRoot.Builder builder = DnsRoot.TreeRoot.newBuilder();
    builder.setERoot(ByteString.copyFrom(eRoot.getBytes()));
    builder.setLRoot(ByteString.copyFrom(lRoot.getBytes()));
    builder.setSeq(seq);

    DnsRoot.Builder dnsRootBuilder = DnsRoot.newBuilder();
    dnsRootBuilder.setTreeRoot(builder.build());
    this.dnsRoot = dnsRootBuilder.build();
  }

  public static RootEntry parseEntry(String e) throws DnsException {
    String value = e.substring(rootPrefix.length());
    DnsRoot dnsRoot1;
    try {
      dnsRoot1 = DnsRoot.parseFrom(Algorithm.decode64(value));
    } catch (InvalidProtocolBufferException ex) {
      throw new DnsException(TypeEnum.INVALID_ROOT, String.format("proto=[%s]", e), ex);
    }

    byte[] signature = Algorithm.decode64(new String(dnsRoot1.getSignature().toByteArray()));
    if (signature.length != 65) {
      throw new DnsException(TypeEnum.INVALID_SIGNATURE,
          String.format("signature's length(%d) != 65, signature: %s", signature.length,
              ByteArray.toHexString(signature)));
    }

    return new RootEntry(dnsRoot1);
  }

  public static RootEntry parseEntry(String e, String publicKey, String domain)
      throws SignatureException, DnsException {
    log.info("Domain:{}, public key:{}", domain, publicKey);
    RootEntry rootEntry = parseEntry(e);
    boolean verify = Algorithm.verifySignature(publicKey, rootEntry.toString(),
        rootEntry.getSignature());
    if (!verify) {
      throw new DnsException(TypeEnum.INVALID_SIGNATURE,
          String.format("verify signature failed! data:[%s], publicKey:%s, domain:%s", e, publicKey,
              domain));
    }
    if (!Algorithm.isValidHash(rootEntry.getERoot()) || !Algorithm.isValidHash(
        rootEntry.getLRoot())) {
      throw new DnsException(TypeEnum.INVALID_CHILD,
          "eroot:" + rootEntry.getERoot() + " lroot:" + rootEntry.getLRoot());
    }
    log.info("Get dnsRoot:[{}]", rootEntry.dnsRoot.toString());
    return rootEntry;
  }

  @Override
  public String toString() {
    return dnsRoot.getTreeRoot().toString();
  }

  public String toFormat() {
    return rootPrefix + Algorithm.encode64(dnsRoot.toByteArray());
  }
}
