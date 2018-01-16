/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
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
