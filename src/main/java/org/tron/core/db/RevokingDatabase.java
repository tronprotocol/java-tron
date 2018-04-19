package org.tron.core.db;

import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.db.AbstractRevokingStore.RevokingState;
import org.tron.core.db.AbstractRevokingStore.RevokingTuple;
import org.tron.core.exception.RevokingStoreIllegalStateException;

public interface RevokingDatabase {

  Dialog buildDialog();

  Dialog buildDialog(boolean forceEnable);

  void onCreate(RevokingTuple tuple, byte[] value);

  void onModify(RevokingTuple tuple, byte[] value);

  void onRemove(RevokingTuple tuple, byte[] value);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  RevokingState head();

  void enable();

  int size();

  void disable();

  void shutdown();
}
