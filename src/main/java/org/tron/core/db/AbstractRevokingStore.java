package org.tron.core.db;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
abstract class AbstractRevokingStore implements RevokingDatabase {
  private static final int DEFAULT_STACK_MAX_SIZE = 256;
  
  private Deque<RevokingState> stack = new LinkedBlockingDeque<>();
  private boolean disabled = true;
  private int activeDialog = 0;
  
  @Override
  public Dialog buildDialog() {
    return buildDialog(false);
  }
  
  @Override
  public Dialog buildDialog(boolean forceEnable) {
    if (disabled && !forceEnable) {
      return new Dialog(this);
    }
    
    boolean disableOnExit = disabled && forceEnable;
    if (forceEnable) {
      enable();
    }
    
    while (stack.size() > DEFAULT_STACK_MAX_SIZE) {
      stack.poll();
    }
    
    stack.add(new RevokingState());
    ++activeDialog;
    return new Dialog(this, disableOnExit);
  }
  
  @Override
  public void onCreate(RevokingTuple tuple, byte[] value) {
  
  }
  
  @Override
  public void onModify(RevokingTuple tuple, byte[] value) {
  
  }
  
  @Override
  public void onRemove(RevokingTuple tuple, byte[] value) {
  
  }
  
  
  @Override
  public void merge() {
  
  }
  
  @Override
  public void revoke() {
  
  }
  
  @Override
  public void commit() {
  
  }
  
  @Override
  public void pop() {
  
  }
  
  @Override
  public RevokingState head() {
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
    private RevokingDatabase revokingDatabase;
    private boolean applyRevoking = true;
    private boolean disableOnExit = false;
    
    public Dialog(Dialog dialog) {
      this.revokingDatabase = dialog.revokingDatabase;
      this.applyRevoking = dialog.applyRevoking;
      dialog.applyRevoking = false;
    }
    
    public Dialog(RevokingDatabase revokingDatabase) {
      this(revokingDatabase, false);
    }
    
    public Dialog(RevokingDatabase revokingDatabase, boolean disbaleOnExit) {
      this.revokingDatabase = revokingDatabase;
      this.disableOnExit = disbaleOnExit;
    }
    
    void commit() {
      applyRevoking = false;
      revokingDatabase.commit();
    }
    
    void takeback() {
      if (applyRevoking) {
        revokingDatabase.revoke();
      }
      
      applyRevoking = false;
    }
    
    void merge() {
      if (applyRevoking) {
        revokingDatabase.merge();
      }
      
      applyRevoking = false;
    }
    
    void copy(Dialog dialog) {
      if (this.equals(dialog)) {
        return;
      }
      
      if (applyRevoking) {
        revokingDatabase.revoke();
      }
      applyRevoking = dialog.applyRevoking;
      dialog.applyRevoking = false;
    }
  }
  
  @Data
  class RevokingState {
    HashMap<RevokingTuple, byte[]> oldValues = new HashMap<>();
    HashSet<RevokingTuple> newIds = new HashSet<>();
    HashMap<RevokingTuple, byte[]> removed = new HashMap<>();
  }
}
