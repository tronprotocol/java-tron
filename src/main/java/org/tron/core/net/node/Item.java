package org.tron.core.net.node;

import lombok.Getter;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Getter
public class Item {

  private Sha256Hash hash;

  private InventoryType type;

  public Item(Sha256Hash hash, InventoryType type) {
    this.hash = hash;
    this.type = type;
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
    return hash.equals(item.getHash()) &&
        type.equals(item.getType());
  }

  @Override
  public int hashCode() {
    return hash.hashCode();
  }
}
