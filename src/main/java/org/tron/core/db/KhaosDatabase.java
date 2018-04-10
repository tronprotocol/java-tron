package org.tron.core.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.util.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.UnLinkedBlockException;

public class KhaosDatabase extends TronDatabase {

  private class KhaosBlock {

    public Sha256Hash getParentHash() {
      return this.blk.getParentHash();
    }

    public KhaosBlock(BlockCapsule blk) {
      this.blk = blk;
      this.id = blk.getBlockId();
      this.num = blk.getNum();
    }

    BlockCapsule blk;
    KhaosBlock parent;
    BlockId id;
    Boolean invalid;
    long num;

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

  private class KhaosStore {

    private HashMap<BlockId, KhaosBlock> hashKblkMap = new HashMap<>();
    //private HashMap<Sha256Hash, KhaosBlock> parentHashKblkMap = new HashMap<>();
    private int maxCapcity = 1024;

    private LinkedHashMap<Long, ArrayList<KhaosBlock>> numKblkMap =
        new LinkedHashMap<Long, ArrayList<KhaosBlock>>() {

          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<KhaosBlock>> entry) {
            if (size() > maxCapcity) {
              entry.getValue().forEach(b -> hashKblkMap.remove(b.id));
              return true;
            }
            return false;
          }
        };


    public void setMaxCapcity(int maxCapcity) {
      this.maxCapcity = maxCapcity;
    }

    public void insert(KhaosBlock block) {
      hashKblkMap.put(block.id, block);
      numKblkMap.computeIfAbsent(block.num, listBlk -> new ArrayList<>())
                .add(block);
    }

    public boolean remove(Sha256Hash hash) {
      KhaosBlock block = this.hashKblkMap.get(hash);
      //Sha256Hash parentHash = Sha256Hash.ZERO_HASH;
      if (block != null) {
        long num = block.num;
        //parentHash = block.getParentHash();
        ArrayList<KhaosBlock> listBlk = numKblkMap.get(num);
        if (listBlk != null) {
          listBlk.removeIf(b -> b.id == hash);
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
  }

  private KhaosBlock head;

  private KhaosStore miniStore = new KhaosStore();

  private KhaosStore miniUnlinkedStore = new KhaosStore();

  protected KhaosDatabase(String dbName) {
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

  void setHead(KhaosBlock blk) {
    this.head = blk;
  }

  void removeBlk(Sha256Hash hash) {
    if (!miniStore.remove(hash)) {
      miniUnlinkedStore.remove(hash);
    }
  }

  /**
   * check if the id is contained in the KhoasDB.
   */
  public Boolean containBlock(Sha256Hash hash) {
    return miniStore.getByHash(hash) != null || miniUnlinkedStore.getByHash(hash) != null;
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
  public BlockCapsule push(BlockCapsule blk) throws UnLinkedBlockException {
    KhaosBlock block = new KhaosBlock(blk);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      KhaosBlock kblock = miniStore.getByHash(block.getParentHash());
      if (kblock != null) {
        block.parent = kblock;
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

  public BlockCapsule getHead() {
    return head.blk;
  }

  /**
   * pop the head block then remove it.
   */
  public boolean pop() {
    KhaosBlock prev = head.parent;
    if (prev != null) {
      head = prev;
      return true;
    }
    return false;
  }

  /**
   * Find two block's most recent common parent block.
   */
  public Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> getBranch(BlockId block1,
      BlockId block2) {
    LinkedList<BlockCapsule> list1 = new LinkedList<>();
    LinkedList<BlockCapsule> list2 = new LinkedList<>();
    KhaosBlock kblk1 = miniStore.getByHash(block1);
    KhaosBlock kblk2 = miniStore.getByHash(block2);

    if (kblk1 != null && kblk2 != null) {
      while (!Objects.equals(kblk1, kblk2)) {
        if (kblk1.num > kblk2.num) {
          list1.add(kblk1.blk);
          kblk1 = kblk1.parent;
        } else if (kblk1.num < kblk2.num) {
          list2.add(kblk2.blk);
          kblk2 = kblk2.parent;
        } else {
          list1.add(kblk1.blk);
          list2.add(kblk2.blk);
          kblk1 = kblk1.parent;
          kblk2 = kblk2.parent;
        }
      }
    }

    return new Pair<>(list1, list2);
  }

  public boolean hasData() {
    return !this.miniStore.hashKblkMap.isEmpty();
  }
}
