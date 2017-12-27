/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
