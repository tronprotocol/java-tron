package org.tron.core.db;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.storage.SourceInter;
import org.tron.common.utils.Utils;

@Slf4j
abstract class AbstractRevokingStore implements RevokingDatabase {
  private static final int DEFAULT_STACK_MAX_SIZE = 256;

  private Deque<RevokingState> stack = new LinkedList<>();
  private boolean disabled = true;
  private int activeDialog = 0;

  public static RevokingDatabase getInstance() {
    return RevokingEnum.INSTANCE.getInstance();
  }

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
    if (activeDialog == 1 && stack.size() == 1) {
      stack.pollLast();
      --activeDialog;
    }
    
    RevokingState state = stack.peekLast();
    @SuppressWarnings("unchecked")
    List<RevokingState> list = (List<RevokingState>) stack;
    RevokingState prevState = list.get(stack.size() - 2);
    
    state.oldValues.entrySet().stream()
        .filter(e -> !prevState.newIds.contains(e.getKey()))
        .filter(e -> !prevState.oldValues.containsKey(e.getKey()))
        .forEach(e -> prevState.oldValues.put(e.getKey(), e.getValue()));
  
    prevState.newIds.addAll(state.newIds);
    
    state.removed.entrySet().stream()
        .filter(e -> {
          boolean has = prevState.newIds.contains(e.getKey());
          if (has) {
            prevState.newIds.remove(e.getKey());
          }
          
          return !has;
        })
        .filter(e -> {
          boolean has = prevState.oldValues.containsKey(e.getKey());
          if (has) {
            prevState.removed.put(e.getKey(), e.getValue());
            prevState.oldValues.remove(e.getKey());
          }
          
          return !has;
        })
        .forEach(e -> prevState.removed.put(e.getKey(), e.getValue()));
    
    stack.pollLast();
    --activeDialog;
  }
  
  @Override
  public void revoke() {
    pop();
    --activeDialog;
  }
  
  @Override
  public void commit() {
    --activeDialog;
  }
  
  @Override
  public void pop() {
    disable();
  
    try {
      RevokingState state = stack.peekLast();
      if (Objects.isNull(state)) {
        return;
      }
  
      state.oldValues.forEach((k, v) -> k.database.putData(k.key, v));
      state.newIds.forEach(e -> e.database.deleteData(e.key));
      state.removed.forEach((k, v) -> k.database.putData(k.key, v));
      stack.pollLast();
    } finally {
      enable();
    }
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
  
  @AllArgsConstructor
  public static class RevokingTuple {
    private SourceInter<byte[], byte[]> database;
    private byte[] key;
  }

  private enum RevokingEnum {
    INSTANCE;

    private RevokingDatabase instance;

    RevokingEnum() {
      instance = new RevokingStore();
    }

    private RevokingDatabase getInstance() {
      return instance;
    }
  }
}
