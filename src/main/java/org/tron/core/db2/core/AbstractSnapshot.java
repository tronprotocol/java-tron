package org.tron.core.db2.core;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import lombok.Getter;
import lombok.Setter;
import org.tron.core.db2.common.DB;

public abstract class AbstractSnapshot<K, V> implements Snapshot {
  @Getter
  protected DB<K, V> db;
  @Getter
  @Setter
  protected Snapshot previous;

  protected WeakReference<Snapshot> next;

  protected Reference<Snapshot> solidity;

  protected Reference<Snapshot> cause;

  @Override
  public void resetSolidity() {
    solidity = new WeakReference<>(getRoot());
  }

  @Override
  public void updateSolidity(Snapshot cause) {
    solidity = new WeakReference<>(solidity.get().getNext());
    if (solidity.get() != null) {
      solidity.get().setCause(cause);
    }
  }

  @Override
  public Snapshot getSolidity() {
    return solidity == null || solidity.get() == null ? getRoot() : solidity.get();
  }

  @Override
  public Snapshot getCause() {
    return cause == null ? null : cause.get();
  }

  @Override
  public void setCause(Snapshot cause) {
    this.cause = new WeakReference<>(cause);
  }


  @Override
  public Snapshot advance() {
    return new SnapshotImpl(this);
  }

  @Override
  public Snapshot getNext() {
    return next == null ? null : next.get();
  }

  @Override
  public void setNext(Snapshot next) {
    this.next = new WeakReference<>(next);
  }
}
