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


public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements
        SourceInter<Key, Value> {

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
