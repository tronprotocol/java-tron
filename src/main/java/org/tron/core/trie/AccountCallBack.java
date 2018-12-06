package org.tron.core.trie;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.db.AccountStateStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
@Component
public class AccountCallBack {

  public static final Cache<String, byte[]> rootHashCache = CacheBuilder.newBuilder()
      .initialCapacity(100).maximumSize(100).build();

  private BlockCapsule blockCapsule;
  private boolean execute = false;
  private TrieImpl trie;

  @Setter
  private Manager manager;

  @Autowired
  private AccountStateStore db;

  public void callBack(byte[] key, AccountCapsule item) {
    if (!exe()) {
      return;
    }
    trie.put(RLP.encodeString(Wallet.encode58Check(key)), item.getData());
  }

  public void preExecute(BlockCapsule blockCapsule) {
    this.blockCapsule = blockCapsule;
    this.execute = true;
    if (!exe()) {
      return;
    }
    byte[] rootHash = null;
    try {
      BlockCapsule parentBlockCapsule = manager.getBlockById(blockCapsule.getParentBlockId());
      rootHash = parentBlockCapsule.getInstance().getBlockHeader().getRawData()
          .getAccountStateRoot().toByteArray();
    } catch (BadItemException e) {
      e.printStackTrace();
    } catch (ItemNotFoundException e) {
      e.printStackTrace();
    }
    trie = new TrieImpl(db, rootHash);
  }

  public void executePushFinish() throws BadBlockException {
    if (!exe()) {
      return;
    }
    ByteString oldRoot = blockCapsule.getInstance().getBlockHeader().getRawData()
        .getAccountStateRoot();
    execute = false;
    byte[] newRoot = trie.getRootHash();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    if (oldRoot.isEmpty()) {
//      blockCapsule.setAccountStateRoot(newRoot);
    } else if (!Arrays.equals(oldRoot.toByteArray(), newRoot)) {
      logger.error("The accountStateRoot hash is not validated. {}, oldRoot: {}, newRoot: {}",
          blockCapsule.getBlockId().getString(), ByteUtil.toHexString(oldRoot.toByteArray()),
          ByteUtil.toHexString(newRoot));
      throw new BadBlockException("The accountStateRoot hash is not validated");
    }
  }

  public void executeGenerateFinish() {
    if (!exe()) {
      return;
    }
    byte[] newRoot = trie.getRootHash();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    blockCapsule.setAccountStateRoot(newRoot);
    execute = false;
  }

  public void exceptionFinish() {
    execute = false;
  }

  private boolean exe() {
    if (!execute || blockCapsule.getNum() < 0) {
      //Agreement same block high to generate account state root
      return false;
    }
    return true;
  }
}
