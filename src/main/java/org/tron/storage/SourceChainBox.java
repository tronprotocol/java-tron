package org.tron.storage;


import java.util.ArrayList;
import java.util.List;


public class SourceChainBox<Key, Value, SourceKey, SourceValue> extends
    AbstractChainedSource<Key, Value, SourceKey,
        SourceValue> {


  List<SourceInter> chain = new ArrayList<SourceInter>();
  SourceInter<Key, Value> lastSourceInter;

  public SourceChainBox(SourceInter<SourceKey, SourceValue> sourceInter) {
    super(sourceInter);
  }

  @Override
  protected boolean flushImpl() {
    return false;
  }

  /**
   * Adds next SourceInter in the chain to the collection
   * Sources should be added from most bottom (connected to the backing SourceInter)
   * All calls to the SourceChainBox will be delegated to the last added
   * SourceInter
   */
  public void add(SourceInter src) {
    chain.add(src);
    lastSourceInter = src;
  }

  @Override
  public void putData(Key key, Value val) {
    lastSourceInter.putData(key, val);
  }

  @Override
  public Value getData(Key key) {
    return lastSourceInter.getData(key);
  }

  @Override
  public void deleteData(Key key) {
    lastSourceInter.deleteData(key);
  }

  @Override
  public boolean flush() {
    return false;
  }


}
