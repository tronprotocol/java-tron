package org.tron.core.db.fast;

import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;
import java.util.Arrays;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.fast.storetrie.AccountStateStoreTrie;
import org.tron.core.db2.common.DB;
import org.tron.core.trie.TrieImpl;

@Slf4j
@Component
public class TrieService {

  @Setter
  private Manager manager;

  @Setter
  private AccountStateStoreTrie accountStateStoreTrie;

  public byte[] getFullAccountStateRootHash() {
    long latestNumber = manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    return getAccountStateRootHash(latestNumber);
  }

  public byte[] getSolidityAccountStateRootHash() {
    long latestSolidityNumber = manager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    return getAccountStateRootHash(latestSolidityNumber);
  }

  private byte[] getAccountStateRootHash(long blockNumber) {
    long latestNumber = blockNumber;
    byte[] rootHash = null;
    try {
      BlockCapsule blockCapsule = manager.getBlockByNum(latestNumber);
      ByteString value = blockCapsule.getInstance().getBlockHeader().getRawData()
          .getAccountStateRoot();
      rootHash = value == null ? null : value.toByteArray();
      if (Arrays.equals(rootHash, Internal.EMPTY_BYTE_ARRAY)) {
        rootHash = Hash.EMPTY_TRIE_HASH;
      }
    } catch (Exception e) {
      logger.error("Get the {} block error.", latestNumber, e);
    }
    return rootHash;
  }

  public TrieImpl getAccountStateTrie() {
    return new TrieImpl(accountStateStoreTrie, getFullAccountStateRootHash());
  }

  public TrieImpl getSolidityAccountStateTrie() {
    return new TrieImpl(accountStateStoreTrie, getSolidityAccountStateRootHash());
  }

  public TrieImpl getChildTrie(byte[] key, DB<byte[], BytesCapsule> db) {
    TrieImpl accountStateTrie = getAccountStateTrie();
    return new TrieImpl(db, accountStateTrie.get(key));
  }

  public TrieImpl getSolidityChildTrie(byte[] key, DB<byte[], BytesCapsule> db) {
    TrieImpl accountStateTrie = getSolidityAccountStateTrie();
    return new TrieImpl(db, accountStateTrie.get(key));
  }
}
