package org.tron.core.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;

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
    Sha256Hash id;
    Boolean invalid;
    long num;
  }

  private class KhaosStore {

    private HashMap<Sha256Hash, KhaosBlock> hashKblkMap = new HashMap<>();
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
      //parentHashKblkMap.put(block.getParentHash(), block);
      ArrayList<KhaosBlock> listBlk = numKblkMap.get(block.num);
      if (listBlk == null) {
        listBlk = new ArrayList<KhaosBlock>();
      }
      listBlk.add(block);
      numKblkMap.put(block.num, listBlk);
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
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

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
    if (miniStore.getByHash(hash) != null) {
      return true;
    }
    return miniUnlinkedStore.getByHash(hash) != null;
  }

  /**
   * Get the Block form KhoasDB, if it doesn't exist ,return null.
   */
  public BlockCapsule getBlock(Sha256Hash hash) {
    KhaosBlock block = miniStore.getByHash(hash);
    if (block != null) {
      return block.blk;
    } else {
      KhaosBlock blockUnlinked = miniStore.getByHash(hash);
      if (blockUnlinked != null) {
        return blockUnlinked.blk;
      } else {
        return null;
      }
    }
  }

  /**
   * Push the block in the KhoasDB.
   */
  public BlockCapsule push(BlockCapsule blk) {
    KhaosBlock block = new KhaosBlock(blk);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      KhaosBlock kblock = miniStore.getByHash(block.getParentHash());
      if (kblock != null) {
        block.parent = kblock;
      } else {
        //unlinked
        miniUnlinkedStore.insert(block);
        return head.blk;
      }
    }

    miniStore.insert(block);

    if (block == null || block.num > head.num) {
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
    miniStore.remove(head.id);
    if (prev != null) {
      head = prev;
      return true;
    }
    return false;
  }

  /**
   * Find two block's most recent common parent block.
   */
  public Pair<ArrayList<BlockCapsule>, ArrayList<BlockCapsule>> getBranch(Sha256Hash block1,
      Sha256Hash block2) {
    List<BlockCapsule> list1 = new ArrayList<>();
    List<BlockCapsule> list2 = new ArrayList<>();
    Pair<ArrayList<BlockCapsule>, ArrayList<BlockCapsule>> ret = new Pair(list1, list2);
    KhaosBlock kblk1 = miniStore.getByHash(block1);
    KhaosBlock kblk2 = miniStore.getByHash(block2);

    if (kblk1 != null && kblk2 != null) {
      do {

        if (kblk1.num > kblk2.num) {
          list1.add(kblk1.blk);
          kblk1 = kblk1.parent;
          continue;
        } else if (kblk1.num < kblk2.num) {
          list2.add(kblk2.blk);
          kblk2 = kblk2.parent;
          continue;
        }

        list1.add(kblk1.blk);
        list2.add(kblk2.blk);
        kblk1 = kblk1.parent;
        kblk2 = kblk2.parent;
      } while (kblk1 != kblk2);
    }
    return ret;
  }

}
