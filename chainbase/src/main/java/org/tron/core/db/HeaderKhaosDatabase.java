package org.tron.core.db;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.protos.Protocol.BlockHeader;

public class HeaderKhaosDatabase extends TronDatabase {

  private KhaosBlock head;
  @Getter
  private KhaosStore miniStore = new KhaosStore();
  @Getter
  private KhaosStore miniUnlinkedStore = new KhaosStore();

  public HeaderKhaosDatabase(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, Object item) {
  }

  @Override
  public void delete(byte[] key) {
  }

  @Override
  public Object get(byte[] key) {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  public void start(BlockHeader blk) {
    this.head = new KhaosBlock(blk);
    miniStore.insert(this.head);
  }

  public void removeBlk(String blockHash) {
    if (!miniStore.remove(blockHash)) {
      miniUnlinkedStore.remove(blockHash);
    }

    head = miniStore.numKblkMap.entrySet().stream()
        .max(Comparator.comparingLong(Map.Entry::getKey))
        .map(Map.Entry::getValue)
        .map(list -> list.get(0))
        .orElseThrow(() -> new RuntimeException("khaosDB head should not be null."));
  }

  /**
   * check if the id is contained in the KhoasDB.
   */
  public Boolean containBlock(Sha256Hash hash) {
    return miniStore.getByHash(hash.toString()) != null
        || miniUnlinkedStore.getByHash(hash.toString()) != null;
  }

  public Boolean containBlockInMiniStore(Sha256Hash hash) {
    return miniStore.getByHash(hash.toString()) != null;
  }

  /**
   * Get the Block form KhoasDB, if it doesn't exist ,return null.
   */
  public BlockHeader getBlock(Sha256Hash hash) {
    return Stream
        .of(miniStore.getByHash(hash.toString()), miniUnlinkedStore.getByHash(hash.toString()))
        .filter(Objects::nonNull)
        .map(block -> block.blk)
        .findFirst()
        .orElse(null);
  }

  /**
   * Push the block in the KhoasDB.
   */
  public BlockHeader push(String chainId, BlockHeader header)
      throws UnLinkedBlockException, BadNumberBlockException {
    KhaosBlock block = new KhaosBlock(header);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      KhaosBlock kblock = miniStore.getByHash(block.getParentHash().toString());
      if (kblock != null) {
        if (header.getRawData().getNumber() != kblock.num + 1) {
          throw new BadNumberBlockException(
              "parent number :" + kblock.num + ",block number :" + header.getRawData().getNumber());
        }
        block.setParent(kblock);
      } else {
        miniUnlinkedStore.insert(block);
        throw new UnLinkedBlockException();
      }
    }

    miniStore.insert(block);

    if (head == null || block.num > head.num) {
      head = block;
    }
    return head.blk;
  }

  public BlockHeader getHead() {
    return head.blk;
  }

  void setHead(KhaosBlock blk) {
    this.head = blk;
  }

  /**
   * pop the head block then remove it.
   */
  public boolean pop() {
    KhaosBlock prev = head.getParent();
    if (prev != null) {
      head = prev;
      return true;
    }
    return false;
  }

  public void setMaxSize(int maxSize) {
    miniUnlinkedStore.setMaxCapcity(maxSize);
    miniStore.setMaxCapcity(maxSize);
  }

  /**
   * Find two block's most recent common parent block.
   */
  public Pair<LinkedList<KhaosBlock>, LinkedList<KhaosBlock>> getBranch(Sha256Hash block1,
      Sha256Hash block2)
      throws NonCommonBlockException {
    LinkedList<KhaosBlock> list1 = new LinkedList<>();
    LinkedList<KhaosBlock> list2 = new LinkedList<>();
    KhaosBlock kblk1 = miniStore.getByHash(block1.toString());
    checkNull(kblk1);
    KhaosBlock kblk2 = miniStore.getByHash(block2.toString());
    checkNull(kblk2);

    while (kblk1.num > kblk2.num) {
      list1.add(kblk1);
      kblk1 = kblk1.getParent();
      checkNull(kblk1);
      checkNull(miniStore.getByHash(kblk1.blockHash));
    }

    while (kblk2.num > kblk1.num) {
      list2.add(kblk2);
      kblk2 = kblk2.getParent();
      checkNull(kblk2);
      checkNull(miniStore.getByHash(kblk2.blockHash));
    }

    while (!Objects.equals(kblk1, kblk2)) {
      list1.add(kblk1);
      list2.add(kblk2);
      kblk1 = kblk1.getParent();
      checkNull(kblk1);
      checkNull(miniStore.getByHash(kblk1.blockHash));
      kblk2 = kblk2.getParent();
      checkNull(kblk2);
      checkNull(miniStore.getByHash(kblk2.blockHash));
    }

    return new Pair<>(list1, list2);
  }

  private void checkNull(Object o) throws NonCommonBlockException {
    if (o == null) {
      throw new NonCommonBlockException();
    }
  }

  /**
   * Find two block's most recent common parent block.
   */
  @Deprecated
  public Pair<LinkedList<BlockHeader>, LinkedList<BlockHeader>> getBranch(
      BlockId block1, BlockId block2) {
    LinkedList<BlockHeader> list1 = new LinkedList<>();
    LinkedList<BlockHeader> list2 = new LinkedList<>();
    KhaosBlock kblk1 = miniStore.getByHash(block1.getString());
    KhaosBlock kblk2 = miniStore.getByHash(block2.getString());

    if (kblk1 != null && kblk2 != null) {
      while (!Objects.equals(kblk1, kblk2)) {
        if (kblk1.num > kblk2.num) {
          list1.add(kblk1.blk);
          kblk1 = kblk1.getParent();
        } else if (kblk1.num < kblk2.num) {
          list2.add(kblk2.blk);
          kblk2 = kblk2.getParent();
        } else {
          list1.add(kblk1.blk);
          list2.add(kblk2.blk);
          kblk1 = kblk1.getParent();
          kblk2 = kblk2.getParent();
        }
      }
    }

    return new Pair<>(list1, list2);
  }

  public boolean hasData() {
    return !this.miniStore.hashKblkMap.isEmpty();
  }

  public static class KhaosBlock {

    @Getter
    private BlockHeader blk;
    private Reference<KhaosBlock> parent = new WeakReference<>(null);
    private String blockHash;
    private Boolean invalid;
    private long num;

    public KhaosBlock(BlockHeader blk) {
      this.blk = blk;
      this.num = blk.getRawData().getNumber();
      this.blockHash = Sha256Hash.of(blk.getRawData().toByteArray()).toString();
    }

    public Sha256Hash getParentHash() {
      return Sha256Hash.wrap(this.blk.getRawData().getParentHash());
    }

    public KhaosBlock getParent() {
      return parent == null ? null : parent.get();
    }

    public void setParent(KhaosBlock parent) {
      this.parent = new WeakReference<>(parent);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      KhaosBlock that = (KhaosBlock) o;
      return StringUtils.equals(blockHash, that.blockHash);
    }

    @Override
    public int hashCode() {
      return Objects.hash(blockHash);
    }
  }

  public class KhaosStore {

    private HashMap<String, KhaosBlock> hashKblkMap = new HashMap<>();
    // private HashMap<Sha256Hash, KhaosBlock> parentHashKblkMap = new HashMap<>();
    private int maxCapcity = 1024;

    @Getter
    private LinkedHashMap<Long, ArrayList<KhaosBlock>> numKblkMap =
        new LinkedHashMap<Long, ArrayList<KhaosBlock>>() {

          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<KhaosBlock>> entry) {
            long minNum = Long.max(0L, head.num - maxCapcity);
            Map<Long, ArrayList<KhaosBlock>> minNumMap = numKblkMap.entrySet().stream()
                .filter(e -> e.getKey() < minNum)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            minNumMap.forEach((k, v) -> {
              numKblkMap.remove(k);
              v.forEach(b -> hashKblkMap.remove(b.blockHash));
            });

            return false;
          }
        };

    public void setMaxCapcity(int maxCapcity) {
      this.maxCapcity = maxCapcity;
    }

    public void insert(KhaosBlock block) {
      hashKblkMap.put(block.blockHash, block);
      numKblkMap.computeIfAbsent(block.num, listBlk -> new ArrayList<>()).add(block);
    }

    public boolean remove(String blockHash) {
      KhaosBlock block = this.hashKblkMap.get(blockHash);
      // Sha256Hash parentHash = Sha256Hash.ZERO_HASH;
      if (block != null) {
        long num = block.num;
        // parentHash = block.getParentHash();
        ArrayList<KhaosBlock> listBlk = numKblkMap.get(num);
        if (listBlk != null) {
          listBlk.removeIf(b -> b.blockHash.equals(blockHash));
        }

        if (CollectionUtils.isEmpty(listBlk)) {
          numKblkMap.remove(num);
        }

        this.hashKblkMap.remove(blockHash);
        return true;
      }
      return false;
    }

    public KhaosBlock getByHash(String hash) {
      return hashKblkMap.get(hash);
    }

    public int size() {
      return hashKblkMap.size();
    }

  }
}
