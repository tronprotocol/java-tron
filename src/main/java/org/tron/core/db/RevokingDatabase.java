package org.tron.core.db;

import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.db.AbstractRevokingStore.RevokingState;
import org.tron.core.db.AbstractRevokingStore.RevokingTuple;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.core.ISession;
import org.tron.core.exception.RevokingStoreIllegalStateException;

public interface RevokingDatabase {

  ISession buildSession();

  ISession buildSession(boolean forceEnable);

  void add(IRevokingDB revokingDB);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  void enable();

  int size();

  void check();

  void setMaxSize(int maxSize);

  void disable();

  void shutdown();
}
