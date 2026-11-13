package org.tron.core.net.peer;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.security.SecureRandom;
import lombok.AccessLevel;
import lombok.Getter;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Getter
public class Item {

  private static final HashFunction ID_HASH = newIdHash();

  @Getter
  private Sha256Hash hash;
  @Getter
  private InventoryType type;
  @Getter
  private long time;
  @Getter(AccessLevel.NONE)
  private final int idHash;


  public Item(Sha256Hash hash, InventoryType type) {
    this.hash = hash;
    this.type = type;
    this.time = System.currentTimeMillis();
    this.idHash = ID_HASH.hashBytes(hash.getBytes()).asInt();
  }

  private static HashFunction newIdHash() {
    SecureRandom random = new SecureRandom();
    return Hashing.sipHash24(random.nextLong(), random.nextLong());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    Item item = (Item) o;
    return hash.equals(item.getHash())
        && type.equals(item.getType());
  }

  @Override
  public int hashCode() {
    return idHash;
  }

  @Override
  public String toString() {
    return type + ":" + hash;
  }
}
