package org.tron.datasource;


public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements Source<Key, Value> {

    private Source<SourceKey, SourceValue> source;
    protected boolean flushSource;

    public AbstractChainedSource(Source<SourceKey, SourceValue> source) {
        this.source = source;
    }

    public Source<SourceKey, SourceValue> getSource() {
        return source;
    }

    public synchronized boolean flush() {
        boolean ret = flushImpl();
        if (flushSource) {
            ret |= getSource().flush();
        }
        return ret;
    }

    /**
     * Should be overridden to do actual source flush
     */
    protected abstract boolean flushImpl();

    public void setFlushSource(boolean flushSource) {
        this.flushSource = flushSource;
    }


}
