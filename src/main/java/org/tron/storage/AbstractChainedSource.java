package org.tron.storage;


public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements SourceInter<Key, Value> {

  protected boolean flushSource;
  private SourceInter<SourceKey, SourceValue> sourceInter;

  public AbstractChainedSource(SourceInter<SourceKey, SourceValue> sourceInter) {
    this.sourceInter = sourceInter;
  }

  public SourceInter<SourceKey, SourceValue> getSourceInter() {
    return sourceInter;
  }

  public synchronized boolean flush() {
    boolean ret = flushImpl();
    if (flushSource) {
      ret |= getSourceInter().flush();
    }
    return ret;
  }

  /**
   * Should be overridden to do actual sourceInter flush
   */
  protected abstract boolean flushImpl();

  public void setFlushSource(boolean flushSource) {
    this.flushSource = flushSource;
  }


}
