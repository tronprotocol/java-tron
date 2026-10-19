package org.tron.core.db;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.UnLinkedBlockException;

@Component
@Slf4j(topic = "DB")
public class KhaosDatabase {

  private KhaosBlock head;

  @Getter
  private KhaosStore miniStore = new KhaosStore();

  void start(BlockCapsule blk) {
    this.head = new KhaosBlock(blk, true);
    miniStore.insert(this.head);
  }

  void removeBlk(Sha256Hash hash) {
    miniStore.remove(hash);

    head = miniStore.numKblkMap.entrySet().stream()
        .max(Comparator.comparingLong(Map.Entry::getKey))
        .map(Map.Entry::getValue)
        .map(list -> list.get(0))
        .orElseThrow(() -> new RuntimeException("khaosDB head should not be null."));
  }

  /**
   * check if the id is contained in the KhoasDB.
   */
  public Boolean containBlockInMiniStore(Sha256Hash hash) {
    return miniStore.getByHash(hash) != null;
  }

  /**
   * Get the Block from KhoasDB, if it doesn't exist ,return null.
   */
  public BlockCapsule getBlock(Sha256Hash hash) {
    KhaosBlock block = miniStore.getByHash(hash);
    return block == null ? null : block.getBlk();
  }

  /**
   * Push the block in the KhoasDB.
   */
  public BlockCapsule push(BlockCapsule blk)
      throws UnLinkedBlockException, BadNumberBlockException {
    KhaosBlock block = new KhaosBlock(blk);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      KhaosBlock kblock = miniStore.getByHash(block.getParentHash());
      if (kblock != null) {
        if (blk.getNum() != kblock.num + 1) {
          throw new BadNumberBlockException(
              String.format("parent number: %d , block number: %d", kblock.num, blk.getNum())
              );
        }
        block.setParent(kblock);
      } else {
        logger.error("Unlinked block: {}, parent: {}.", blk.getBlockId(), blk.getParentHash());
        throw new UnLinkedBlockException();
      }
    }

    miniStore.insert(block);

    if (head == null || block.num > head.num) {
      head = block;
    }
    return head == block ? blk : head.getBlk();
  }

  public BlockCapsule getHead() {
    return head.getBlk();
  }

  public BlockId getHeadId() {
    return head.id;
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
    miniStore.setMaxCapacity(maxSize);
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

  public boolean hasData() {
    return !this.miniStore.hashKblkMap.isEmpty();
  }

  public static class KhaosBlock {

    private BlockCapsule blk;
    private byte[] rawData;
    private Reference<KhaosBlock> parent = new WeakReference<>(null);
    @Getter
    private final BlockId id;
    @Getter
    private final long num;
    @Getter
    private final Sha256Hash parentHash;
    private final boolean generatedByMyself;

    public KhaosBlock(BlockCapsule blk) {
      this(blk, false);
    }

    private KhaosBlock(BlockCapsule blk, boolean processed) {
      this.id = blk.getBlockId();
      this.num = blk.getNum();
      this.parentHash = blk.getParentHash();
      this.generatedByMyself = blk.generatedByMyself;
      if (processed) {
        this.blk = blk;
      } else {
        this.rawData = blk.getData();
      }
    }

    /** Queries must not retain decoded objects for an unprocessed block. */
    public synchronized BlockCapsule getBlk() {
      if (blk != null) {
        return blk;
      }
      try {
        BlockCapsule block = new BlockCapsule(rawData);
        block.generatedByMyself = generatedByMyself;
        return block;
      } catch (BadItemException e) {
        throw new IllegalStateException("Cannot decode cached block " + id, e);
      }
    }

    public synchronized boolean isProcessed() {
      return blk != null;
    }

    /** Retain the executed capsule only after its database session commits successfully. */
    synchronized void commit(BlockCapsule block) {
      if (!id.equals(block.getBlockId())) {
        throw new IllegalArgumentException("Committed block ID does not match " + id);
      }
      blk = block;
      rawData = null;
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
  }

  public class KhaosStore {

    private Map<BlockId, KhaosBlock> hashKblkMap = new ConcurrentHashMap<>();
    // private HashMap<Sha256Hash, KhaosBlock> parentHashKblkMap = new HashMap<>();
    private int maxCapacity = 1024;

    @Getter
    private LinkedHashMap<Long, ArrayList<KhaosBlock>> numKblkMap =
        new LinkedHashMap<Long, ArrayList<KhaosBlock>>() {

          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<KhaosBlock>> entry) {
            long minNum = Long.max(0L, head.num - maxCapacity);
            Map<Long, ArrayList<KhaosBlock>> minNumMap = numKblkMap.entrySet().stream()
                .filter(e -> e.getKey() < minNum)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            minNumMap.forEach((k, v) -> {
              numKblkMap.remove(k);
              v.forEach(b -> {
                hashKblkMap.remove(b.id);
                logger.info("Remove from khaosDatabase: {}.", b.id);
              });
            });

            return false;
          }
        };

    public synchronized void setMaxCapacity(int maxCapacity) {
      this.maxCapacity = maxCapacity;
    }

    public synchronized void insert(KhaosBlock block) {
      hashKblkMap.put(block.id, block);
      numKblkMap.computeIfAbsent(block.num, listBlk -> new ArrayList<>()).add(block);
    }

    public synchronized boolean remove(Sha256Hash hash) {
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

    public synchronized List<KhaosBlock> getBlockByNum(Long num) {
      return numKblkMap.get(num);
    }

    public synchronized KhaosBlock getByHash(Sha256Hash hash) {
      return hashKblkMap.get(hash);
    }

    public synchronized int size() {
      return hashKblkMap.size();
    }

  }
}
