package org.tron.core.db;

import org.tron.core.db.AbstractMissDoDatabase.Dialog;
import org.tron.core.db.AbstractMissDoDatabase.MissDoState;

public interface IMissDoDatabase {
  Dialog buildDialog();
  
  void onCreate(MissDoTuple tuple, byte[] value);
  
  void onModify(MissDoTuple tuple, byte[] value);
  
  void onRemove(MissDoTuple tuple, byte[] value);
  
  void merge();
  
  void takeback();
  
  void commit();
  
  void pop_commit();
  
  MissDoState head();
  
  void enable();
  
  void disable();
}
