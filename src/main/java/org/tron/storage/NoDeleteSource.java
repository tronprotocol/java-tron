package org.tron.storage;

public class NoDeleteSource<Key, Value> extends AbstractChainedSource<Key, Value, Key, Value> {

    public NoDeleteSource(SourceInter<Key, Value> src) {
        super(src);
        setFlushSource(true);
    }

    @Override
    public void deleteData(Key key) {
    }

    @Override
    public void putData(Key key, Value val) {
        if (val != null) getSourceInter().putData(key, val);
    }

    @Override
    public Value getData(Key key) {
        return getSourceInter().getData(key);
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }
}
