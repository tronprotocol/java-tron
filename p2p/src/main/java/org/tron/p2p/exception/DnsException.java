package org.tron.p2p.exception;


public class DnsException extends Exception {

  private static final long serialVersionUID = 9096335228978001485L;
  private final DnsException.TypeEnum type;

  public DnsException(DnsException.TypeEnum type, String errMsg) {
    super(type.desc + ", " + errMsg);
    this.type = type;
  }

  public DnsException(DnsException.TypeEnum type, Throwable throwable) {
    super(throwable);
    this.type = type;
  }

  public DnsException(DnsException.TypeEnum type, String errMsg, Throwable throwable) {
    super(errMsg, throwable);
    this.type = type;
  }

  public DnsException.TypeEnum getType() {
    return type;
  }

  public enum TypeEnum {
    LOOK_UP_ROOT_FAILED(0, "look up root failed"),
    //Resolver/sync errors
    NO_ROOT_FOUND(1, "no valid root found"),
    NO_ENTRY_FOUND(2, "no valid tree entry found"),
    HASH_MISS_MATCH(3, "hash miss match"),
    NODES_IN_LINK_TREE(4, "nodes entry in link tree"),
    LINK_IN_NODES_TREE(5, "link entry in nodes tree"),

    // Entry parse errors
    UNKNOWN_ENTRY(6, "unknown entry type"),
    NO_PUBLIC_KEY(7, "missing public key"),
    BAD_PUBLIC_KEY(8, "invalid public key"),
    INVALID_NODES(9, "invalid node list"),
    INVALID_CHILD(10, "invalid child hash"),
    INVALID_SIGNATURE(11, "invalid base64 signature"),
    INVALID_ROOT(12, "invalid DnsRoot proto"),
    INVALID_SCHEME_URL(13, "invalid scheme url"),

    // Publish error
    DEPLOY_DOMAIN_FAILED(14, "failed to deploy domain"),

    OTHER_ERROR(15, "other error");

    private final Integer value;
    private final String desc;

    TypeEnum(Integer value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    public Integer getValue() {
      return value;
    }

    public String getDesc() {
      return desc;
    }

    @Override
    public String toString() {
      return value + "-" + desc;
    }
  }
}
