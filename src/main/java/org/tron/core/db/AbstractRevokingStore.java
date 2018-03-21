package org.tron.core.db;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.storage.SourceInter;
import org.tron.common.utils.Utils;
import org.tron.core.exception.RevokingStoreIllegalStateException;

@Slf4j
@Getter // only for unit test
public abstract class AbstractRevokingStore implements RevokingDatabase {

  private static final int DEFAULT_STACK_MAX_SIZE = 256;

  private Deque<RevokingState> stack = new LinkedList<>();
  private boolean disabled = true;
  private int activeDialog = 0;

  @Override
  public Dialog buildDialog() {
    return buildDialog(false);
  }

  @Override
  public synchronized Dialog buildDialog(boolean forceEnable) {
    if (disabled && !forceEnable) {
      return new Dialog(this);
    }

    boolean disableOnExit = disabled && forceEnable;
    if (forceEnable) {
      disabled = false;
    }

    while (stack.size() > DEFAULT_STACK_MAX_SIZE) {
      stack.poll();
    }

    stack.add(new RevokingState());
    ++activeDialog;
    return new Dialog(this, disableOnExit);
  }

  @Override
  public synchronized void onCreate(RevokingTuple tuple, byte[] value) {
    if (disabled) {
      return;
    }

    addIfEmtpy();
    RevokingState state = stack.peekLast();
    state.newIds.add(tuple);
  }

  @Override
  public synchronized void onModify(RevokingTuple tuple, byte[] value) {
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
  public synchronized void onRemove(RevokingTuple tuple, byte[] value) {
    if (disabled) {
      return;
    }

    addIfEmtpy();
    RevokingState state = stack.peekLast();
    if (state.newIds.contains(tuple)) {
      state.newIds.remove(tuple);
      return;
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
  public synchronized void merge() throws RevokingStoreIllegalStateException {
    if (activeDialog <= 0) {
      throw new RevokingStoreIllegalStateException("activeDialog has to be greater than 0");
    }

    if (activeDialog == 1 && stack.size() == 1) {
      stack.pollLast();
      --activeDialog;
      return;
    }

    if (stack.size() < 2) {
      return;
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
  public synchronized void revoke() throws RevokingStoreIllegalStateException {
    if (disabled) {
      return;
    }

    if (activeDialog <= 0) {
      throw new RevokingStoreIllegalStateException("activeDialog has to be greater than 0");
    }

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
      disabled = false;
    }
    --activeDialog;
  }

  @Override
  public synchronized void commit() throws RevokingStoreIllegalStateException {
    if (activeDialog <= 0) {
      throw new RevokingStoreIllegalStateException("activeDialog has to be greater than 0");
    }

    --activeDialog;
  }

  @Override
  public synchronized void pop() throws RevokingStoreIllegalStateException {
    if (activeDialog != 0) {
      throw new RevokingStoreIllegalStateException("activeDialog has to be equal 0");
    }

    if (stack.isEmpty()) {
      throw new RevokingStoreIllegalStateException("stack is empty");
    }

    disable();

    try {
      RevokingState state = stack.peekLast();
      state.oldValues.forEach((k, v) -> k.database.putData(k.key, v));
      state.newIds.forEach(e -> e.database.deleteData(e.key));
      state.removed.forEach((k, v) -> k.database.putData(k.key, v));
      stack.pollLast();
    } finally {
      disabled = false;
    }
  }

  @Override
  public synchronized RevokingState head() {
    if (stack.isEmpty()) {
      return null;
    }

    return stack.peekLast();
  }

  @Override
  public synchronized void enable() {
    disabled = false;
  }

  @Override
  public synchronized void disable() {
    disabled = true;
  }

  private void addIfEmtpy() {
    if (stack.isEmpty()) {
      stack.add(new RevokingState());
    }
  }

  @Slf4j
  @Getter // only for unit test
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

    void commit() throws RevokingStoreIllegalStateException {
      applyRevoking = false;
      revokingDatabase.commit();
    }

    void revoke() throws RevokingStoreIllegalStateException {
      if (applyRevoking) {
        revokingDatabase.revoke();
      }

      applyRevoking = false;
    }

    void merge() throws RevokingStoreIllegalStateException {
      if (applyRevoking) {
        revokingDatabase.merge();
      }

      applyRevoking = false;
    }

    void copy(Dialog dialog) throws RevokingStoreIllegalStateException {
      if (this.equals(dialog)) {
        return;
      }

      if (applyRevoking) {
        revokingDatabase.revoke();
      }
      applyRevoking = dialog.applyRevoking;
      dialog.applyRevoking = false;
    }

    public void destroy() {
      try {
        if (applyRevoking) {
          revokingDatabase.revoke();
        }
      } catch (Exception e) {
        logger.error("revoke database error.", e);
      }
      if (disableOnExit) {
        revokingDatabase.disable();
      }
    }

    @Override
    public void close() throws RevokingStoreIllegalStateException {
      try {
        if (applyRevoking) {
          revokingDatabase.revoke();
        }
      } catch (Exception e) {
        logger.error("revoke database error.", e);
        throw new RevokingStoreIllegalStateException(e);
      }
      if (disableOnExit) {
        revokingDatabase.disable();
      }
    }
  }

  @ToString
  @Getter // only for unit test
  static class RevokingState {

    Map<RevokingTuple, byte[]> oldValues = new HashMap<>();
    Set<RevokingTuple> newIds = new HashSet<>();
    Map<RevokingTuple, byte[]> removed = new HashMap<>();
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  @Getter
  public static class RevokingTuple {

    private SourceInter<byte[], byte[]> database;
    private byte[] key;
  }

}
