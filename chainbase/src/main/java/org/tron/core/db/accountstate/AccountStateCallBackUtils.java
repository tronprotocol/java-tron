package org.tron.core.db.accountstate;

import java.util.ArrayList;
import java.util.List;
import org.tron.core.capsule.AccountCapsule;

public class AccountStateCallBackUtils {

  protected volatile boolean execute = false;
  protected volatile boolean allowGenerateRoot = false;
  protected List<TrieEntry> trieEntryList = new ArrayList<>();

  public void accountCallBack(byte[] key, AccountCapsule item) {
    if (!exe()) {
      return;
    }
    if (item == null) {
      return;
    }
    trieEntryList
        .add(TrieEntry.build(key, new AccountStateEntity(item.getInstance()).toByteArrays()));
  }

  protected boolean exe() {
    if (!execute || !allowGenerateRoot) {
      //Agreement same block high to generate account state root
      execute = false;
      return false;
    }
    return true;
  }

  public static class TrieEntry {

    private byte[] key;
    private byte[] data;

    public static TrieEntry build(byte[] key, byte[] data) {
      TrieEntry trieEntry = new TrieEntry();
      return trieEntry.setKey(key).setData(data);
    }

    public byte[] getKey() {
      return key;
    }

    public TrieEntry setKey(byte[] key) {
      this.key = key;
      return this;
    }

    public byte[] getData() {
      return data;
    }

    public TrieEntry setData(byte[] data) {
      this.data = data;
      return this;
    }
  }

}
