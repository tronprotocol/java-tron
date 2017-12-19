package org.tron.datasource;


import java.util.ArrayList;
import java.util.List;


public class SourceChainBox<Key, Value, SourceKey, SourceValue> extends AbstractChainedSource<Key, Value, SourceKey,
        SourceValue> {


    List<Source> chain = new ArrayList<Source>();
    Source<Key, Value> lastSource;

    public SourceChainBox(Source<SourceKey, SourceValue> source) {
        super(source);
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }

    /**
     * Adds next Source in the chain to the collection
     * Sources should be added from most bottom (connected to the backing Source)
     * All calls to the SourceChainBox will be delegated to the last added
     * Source
     */
    public void add(Source src) {
        chain.add(src);
        lastSource = src;
    }

    @Override
    public void put(Key key, Value val) {
        lastSource.put(key,val);
    }

    @Override
    public Value get(Key key) {
        return lastSource.get(key);
    }

    @Override
    public void delete(Key key) {
        lastSource.delete(key);
    }

    @Override
    public boolean flush() {
        return false;
    }


}
