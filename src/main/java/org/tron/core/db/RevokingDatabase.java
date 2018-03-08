package org.tron.core.db;

import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.db.AbstractRevokingStore.RevokingState;
import org.tron.core.db.AbstractRevokingStore.RevokingTuple;

public interface RevokingDatabase {
  Dialog buildDialog();
  
  Dialog buildDialog(boolean forceEnable);
  
  void onCreate(RevokingTuple tuple, byte[] value);
  
  void onModify(RevokingTuple tuple, byte[] value);
  
  void onRemove(RevokingTuple tuple, byte[] value);
  
  void merge();
  
  void revoke();
  
  void commit();
  
  void pop();
  
  RevokingState head();
  
  void enable();
  
  void disable();
}
