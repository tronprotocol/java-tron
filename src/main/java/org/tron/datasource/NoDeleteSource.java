package org.tron.datasource;


public class NoDeleteSource<Key, Value> extends AbstractChainedSource<Key, Value, Key, Value> {

    public NoDeleteSource(Source<Key, Value> src) {
        super(src);
        setFlushSource(true);
    }

    @Override
    public void delete(Key key) {
    }

    @Override
    public void put(Key key, Value val) {
        if (val != null) getSource().put(key, val);
    }

    @Override
    public Value get(Key key) {
        return getSource().get(key);
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }
}
