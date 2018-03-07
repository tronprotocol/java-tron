package org.tron.core.db;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
abstract class AbstractMissDoDatabase implements IMissDoDatabase {
   private static final int DEFAULT_STACK_MAX_SIZE = 256;
   
   private Deque<MissDoState> stack = new LinkedBlockingDeque<>();
   private boolean disabled = true;
   private int activeDialog = 0;
   
  @Override
  public Dialog buildDialog() {
    return new Dialog(this);
  }
  
   @Override
   public void onCreate(MissDoTuple tuple, byte[] value) {
   
   }
  
   @Override
   public void onModify(MissDoTuple tuple, byte[] value) {
   
   }
  
   @Override
   public void onRemove(MissDoTuple tuple, byte[] value) {
   
   }
  
  
   @Override
  public void merge() {

  }

  @Override
  public void takeback() {
  
  }
  
  @Override
  public void commit() {

  }

  @Override
  public void pop() {

  }
  
   @Override
   public MissDoState head() {
     return null;
   }
   
   @Override
   public void enable() {
   
   }
  
   @Override
   public void disable() {
   
   }
   
  @Builder
  @Slf4j
  public class Dialog {
    private IMissDoDatabase missDoDatabase;
    private boolean applyMissDo = true;
    private boolean disableOnExit = false;

    public Dialog(Dialog dialog) {
      this.missDoDatabase = dialog.missDoDatabase;
      this.applyMissDo = dialog.applyMissDo;
      dialog.applyMissDo = false;
    }
    
    public Dialog(IMissDoDatabase missDoDatabase) {
      this(missDoDatabase, false);
    }
    
    public Dialog(IMissDoDatabase missDoDatabase, boolean disbaleOnExit) {
      this.missDoDatabase = missDoDatabase;
      this.disableOnExit = disbaleOnExit;
    }
    
    void commit() {
      applyMissDo = false;
      missDoDatabase.commit();
    }
    
    void takeback() {
      if (applyMissDo) {
        missDoDatabase.takeback();
      }
    
      applyMissDo = false;
    }
    
    void merge() {
      if (applyMissDo) {
        missDoDatabase.merge();
      }
      
      applyMissDo = false;
    }

    void copy(Dialog dialog) {
      if (this.equals(dialog)) {
        return;
      }
      
      if (applyMissDo) {
        missDoDatabase.takeback();
      }
      applyMissDo = dialog.applyMissDo;
      dialog.applyMissDo = false;
    }
  }
  
  @Data
  class MissDoState {
    HashMap<MissDoTuple, byte[]> oldValues;
    HashSet<MissDoTuple> newIds;
    HashMap<MissDoTuple, byte[]> removed;
  }
}
