package org.tron.core.db;

import lombok.Builder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Utils;

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
    if (disabled) {
      return;
    }
  
    addIfEmtpy();
    RevokingState state = stack.peekLast();
    state.newIds.add(tuple);
  }
  
  @Override
  public void onModify(RevokingTuple tuple, byte[] value) {
    if (disabled) {
      return;
    }
    
    addIfEmtpy();
    RevokingState state = stack.peekLast();
    if (state.newIds.contains(tuple) || state.oldValues.containsKey(tuple)) {
      return;
    }
    
    state.oldValues.put(tuple, Utils.clone(value));
  }
  
  @Override
  public void onRemove(RevokingTuple tuple, byte[] value) {
    if (disabled) {
      return;
    }
    
    addIfEmtpy();
    RevokingState state = stack.peekLast();
    if (state.newIds.contains(tuple)) {
      state.newIds.remove(tuple);
    }
    
    if (state.oldValues.containsKey(tuple)) {
      state.removed.put(tuple, state.oldValues.get(tuple));
      state.oldValues.remove(tuple);
      return;
    }
    
    if (state.removed.containsKey(tuple)) {
      return;
    }
    
    state.removed.put(tuple, Utils.clone(value));
  }
  
  @Override
  public void merge() {
  
  }
  
  @Override
  public void revoke() {
    disable();
    
    RevokingState state = stack.peekLast();
    
  }
  
  @Override
  public void commit() {
    --activeDialog;
  }
  
  @Override
  public void pop() {
  
  }
  
  @Override
  public RevokingState head() {
    return stack.peekLast();
  }
  
  @Override
  public void enable() {
    disabled = false;
  }
  
  @Override
  public void disable() {
    disabled = true;
  }
  
  private void addIfEmtpy() {
    if (stack.isEmpty()) {
      stack.add(new RevokingState());
    }
  }
  
  @Builder
  @Slf4j
  public static class Dialog implements AutoCloseable {
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
    
    void revoke() {
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
  
    @Override
    public void close() throws Exception {
      try {
        if (applyRevoking) {
          revokingDatabase.revoke();
        }
      } catch (Exception e) {
        log.error("revoke database error.", e);
        throw e;
      }
      if (disableOnExit) revokingDatabase.disable();
    }
  }
  
  @ToString
  static class RevokingState {
    HashMap<RevokingTuple, byte[]> oldValues = new HashMap<>();
    HashSet<RevokingTuple> newIds = new HashSet<>();
    HashMap<RevokingTuple, byte[]> removed = new HashMap<>();
  }
}
