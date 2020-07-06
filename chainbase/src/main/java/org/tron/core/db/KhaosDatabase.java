package org.tron.core.db;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.UnLinkedBlockException;

@Component
@Slf4j(topic = "DB")
public class KhaosDatabase extends TronDatabase {

  public static class Log {
    Object blk_begin;
    Object blk_prev;
    Object head_begin;
    Object head_prev;
    Object mini_store_begin;
    Object mini_store_prev;
    Object maxCapcity_begin;
    Object maxCapcity_prev;
    Object numKblkMap_begin;
    Object numKblkMap_prev;
    Object hashKblkMap_begin;
    Object hashKblkMap_prev;
    Object head_num_begin;
    Object head_num_prev;

    public void setBlk_begin(Object blk_begin) {
      this.blk_prev = this.blk_begin;
      this.blk_begin = blk_begin;
    }

    public void setHead_begin(Object head_begin) {
      this.head_prev = this.head_begin;
      this.head_begin = head_begin;
    }

    public void setMini_store_begin(Object mini_store_begin) {
      this.mini_store_prev = this.mini_store_begin;
      this.mini_store_begin = mini_store_begin;
    }

    public void setMaxCapcity_begin(Object maxCapcity_begin) {
      this.maxCapcity_prev = this.maxCapcity_begin;
      this.maxCapcity_begin = maxCapcity_begin;
    }

    public void setNumKblkMap_begin(Object numKblkMap_begin) {
      this.numKblkMap_prev = this.numKblkMap_begin;
      this.numKblkMap_begin = numKblkMap_begin;
    }

    public void setHashKblkMap_begin(Object hashKblkMap_begin) {
      this.hashKblkMap_prev = this.hashKblkMap_begin;
      this.hashKblkMap_begin = hashKblkMap_begin;
    }

    public void setHead_num_begin(Object head_num_begin) {
      this.head_num_prev = this.head_num_begin;
      this.head_num_begin = head_num_begin;
    }

    public void print() {
      logger.info("********************** " + this.toString());
    }

    @Override
    public String toString() {
      return "Log{" +
          "blk_begin=" + blk_begin +
          ", blk_prev=" + blk_prev +
          ", head_begin=" + head_begin +
          ", head_prev=" + head_prev +
          ", mini_store_begin=" + mini_store_begin +
          ", mini_store_prev=" + mini_store_prev +
          ", maxCapcity_begin=" + maxCapcity_begin +
          ", maxCapcity_prev=" + maxCapcity_prev +
          ", numKblkMap_begin=" + numKblkMap_begin +
          ", numKblkMap_prev=" + numKblkMap_prev +
          ", hashKblkMap_begin=" + hashKblkMap_begin +
          ", hashKblkMap_prev=" + hashKblkMap_prev +
          ", head_num_begin=" + head_num_begin +
          ", head_num_prev=" + head_num_prev +
          '}';
    }
  }

  public static Log log = new Log();

  private KhaosBlock head;
  @Getter
  private KhaosStore miniStore = new KhaosStore();
  @Getter
  private KhaosStore miniUnlinkedStore = new KhaosStore();

  @Autowired
  protected KhaosDatabase(@Value("block_KDB") String dbName) {
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

  void start(BlockCapsule blk) {
    this.head = new KhaosBlock(blk);
    miniStore.insert(this.head);
  }

  void removeBlk(Sha256Hash hash) {
    if (!miniStore.remove(hash)) {
      miniUnlinkedStore.remove(hash);
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
    return miniStore.getByHash(hash) != null || miniUnlinkedStore.getByHash(hash) != null;
  }

  public Boolean containBlockInMiniStore(Sha256Hash hash) {
    return miniStore.getByHash(hash) != null;
  }

  /**
   * Get the Block form KhoasDB, if it doesn't exist ,return null.
   */
  public BlockCapsule getBlock(Sha256Hash hash) {
    return Stream.of(miniStore.getByHash(hash), miniUnlinkedStore.getByHash(hash))
        .filter(Objects::nonNull)
        .map(block -> block.blk)
        .findFirst()
        .orElse(null);
  }

  /**
   * Push the block in the KhoasDB.
   */
  public BlockCapsule push(BlockCapsule blk)
      throws UnLinkedBlockException, BadNumberBlockException {
    log.setBlk_begin(blk);
    log.setHead_begin(blk);
    log.setMini_store_begin(miniStore);
    KhaosBlock block = new KhaosBlock(blk);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      KhaosBlock kblock = miniStore.getByHash(block.getParentHash());
      if (kblock != null) {
        if (blk.getNum() != kblock.num + 1) {
          throw new BadNumberBlockException(
              "parent number :" + kblock.num + ",block number :" + blk.getNum());
        }
        block.setParent(kblock);
      } else {
        log.print();
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

  public BlockCapsule getHead() {
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
    KhaosBlock kblk1 = miniStore.getByHash(block1);
    checkNull(kblk1);
    KhaosBlock kblk2 = miniStore.getByHash(block2);
    checkNull(kblk2);

    while (kblk1.num > kblk2.num) {
      list1.add(kblk1);
      kblk1 = kblk1.getParent();
      checkNull(kblk1);
      checkNull(miniStore.getByHash(kblk1.id));
    }

    while (kblk2.num > kblk1.num) {
      list2.add(kblk2);
      kblk2 = kblk2.getParent();
      checkNull(kblk2);
      checkNull(miniStore.getByHash(kblk2.id));
    }

    while (!Objects.equals(kblk1, kblk2)) {
      list1.add(kblk1);
      list2.add(kblk2);
      kblk1 = kblk1.getParent();
      checkNull(kblk1);
      checkNull(miniStore.getByHash(kblk1.id));
      kblk2 = kblk2.getParent();
      checkNull(kblk2);
      checkNull(miniStore.getByHash(kblk2.id));
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
  public Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> getBranch(
      BlockId block1, BlockId block2) {
    LinkedList<BlockCapsule> list1 = new LinkedList<>();
    LinkedList<BlockCapsule> list2 = new LinkedList<>();
    KhaosBlock kblk1 = miniStore.getByHash(block1);
    KhaosBlock kblk2 = miniStore.getByHash(block2);

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

  // only for unittest
  public BlockCapsule getParentBlock(Sha256Hash hash) {
    return Stream.of(miniStore.getByHash(hash), miniUnlinkedStore.getByHash(hash))
        .filter(Objects::nonNull)
        .map(KhaosBlock::getParent)
        .map(khaosBlock -> khaosBlock == null ? null : khaosBlock.blk)
        .filter(Objects::nonNull)
        .filter(b -> containBlock(b.getBlockId()))
        .findFirst()
        .orElse(null);
  }

  public boolean hasData() {
    return !this.miniStore.hashKblkMap.isEmpty();
  }

  public static class KhaosBlock {

    @Getter
    private BlockCapsule blk;
    private Reference<KhaosBlock> parent = new WeakReference<>(null);
    private BlockId id;
    private Boolean invalid;
    private long num;

    public KhaosBlock(BlockCapsule blk) {
      this.blk = blk;
      this.id = blk.getBlockId();
      this.num = blk.getNum();
    }

    public Sha256Hash getParentHash() {
      return this.blk.getParentHash();
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
      return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "KhaosBlock{" +
          "blk=" + blk +
          ", parent=" + parent.get() +
          ", id=" + id +
          ", num=" + num +
          '}';
    }
  }

  public class KhaosStore {

    private HashMap<BlockId, KhaosBlock> hashKblkMap = new HashMap<>();
    // private HashMap<Sha256Hash, KhaosBlock> parentHashKblkMap = new HashMap<>();
    private int maxCapcity = 1024;

    @Getter
    private LinkedHashMap<Long, ArrayList<KhaosBlock>> numKblkMap =
        new LinkedHashMap<Long, ArrayList<KhaosBlock>>() {

          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<KhaosBlock>> entry) {
            log.setHead_num_begin(head.num);
            log.setMaxCapcity_begin(maxCapcity);
            log.setNumKblkMap_begin(numKblkMap);
            log.setHashKblkMap_begin(hashKblkMap);
            long minNum = Long.max(0L, head.num - maxCapcity);
            Map<Long, ArrayList<KhaosBlock>> minNumMap = numKblkMap.entrySet().stream()
                .filter(e -> e.getKey() < minNum)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            minNumMap.forEach((k, v) -> {
              numKblkMap.remove(k);
              v.forEach(b -> hashKblkMap.remove(b.id));
            });
            return false;
          }
        };

    public void setMaxCapcity(int maxCapcity) {
      this.maxCapcity = maxCapcity;
    }

    public void insert(KhaosBlock block) {
      hashKblkMap.put(block.id, block);
      numKblkMap.computeIfAbsent(block.num, listBlk -> new ArrayList<>()).add(block);
    }

    public boolean remove(Sha256Hash hash) {
      KhaosBlock block = this.hashKblkMap.get(hash);
      // Sha256Hash parentHash = Sha256Hash.ZERO_HASH;
      if (block != null) {
        long num = block.num;
        // parentHash = block.getParentHash();
        ArrayList<KhaosBlock> listBlk = numKblkMap.get(num);
        if (listBlk != null) {
          listBlk.removeIf(b -> b.id.equals(hash));
        }

        if (CollectionUtils.isEmpty(listBlk)) {
          numKblkMap.remove(num);
        }

        this.hashKblkMap.remove(hash);
        return true;
      }
      return false;
    }

    public List<KhaosBlock> getBlockByNum(Long num) {
      return numKblkMap.get(num);
    }

    public KhaosBlock getByHash(Sha256Hash hash) {
      return hashKblkMap.get(hash);
    }

    public int size() {
      return hashKblkMap.size();
    }

    @Override
    public String toString() {
      return "KhaosStore{" +
          "hashKblkMap=" + hashKblkMap +
          ", maxCapcity=" + maxCapcity +
          ", numKblkMap=" + numKblkMap +
          '}';
    }
  }
}
