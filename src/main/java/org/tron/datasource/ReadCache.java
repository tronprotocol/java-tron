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

import org.apache.commons.collections4.map.LRUMap;
import org.tron.db.ByteArrayWrapper;
import org.tron.utils.ByteArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ReadCache<Key, Value> extends AbstractCachedSource<Key, Value> {

    private final Value NULL = (Value) new Object();

    private Map<Key, Value> cache;
    private boolean byteKeyMap;

    public ReadCache(Source<Key, Value> src) {
        super(src);
        withCache(new HashMap<Key, Value>());
    }

    /**
     * Installs the specific cache Map implementation
     */
    public ReadCache<Key, Value> withCache(Map<Key, Value> cache) {
        byteKeyMap = cache instanceof ByteArrayMap;
        this.cache = Collections.synchronizedMap(cache);
        return this;
    }

    /**
     * Sets the max number of entries to cache
     */
    public ReadCache<Key, Value> withMaxCapacity(int maxCapacity) {
        return withCache(new LRUMap<Key, Value>(maxCapacity) {
            @Override
            protected boolean removeLRU(LinkEntry<Key, Value> entry) {
                cacheRemoved(entry.getKey(), entry.getValue());
                return super.removeLRU(entry);
            }
        });
    }

    // the guard against incorrect Map implementation for byte[] keys
    private boolean checked = false;
    private void checkByteArrKey(Key key) {
        if (checked) return;

        if (key instanceof byte[]) {
            if (!byteKeyMap) {
                throw new RuntimeException("Wrong map/set for byte[] key");
            }
        }
        checked = true;
    }

    @Override
    public void put(Key key, Value val) {
        checkByteArrKey(key);
        if (val == null) {
            delete(key);
        } else {
            cache.put(key, val);
            cacheAdded(key, val);
            getSource().put(key, val);
        }
    }

    @Override
    public Value get(Key key) {
        checkByteArrKey(key);
        Value ret = cache.get(key);
        if (ret == NULL) {
            return null;
        }
        if (ret == null) {
            ret = getSource().get(key);
            cache.put(key, ret == null ? NULL : ret);
            cacheAdded(key, ret);
        }
        return ret;
    }

    @Override
    public void delete(Key key) {
        checkByteArrKey(key);
        Value value = cache.remove(key);
        cacheRemoved(key, value);
        getSource().delete(key);
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }

    public synchronized Collection<Key> getModified() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasModified() {
        return false;
    }

    @Override
    public synchronized Entry<Value> getCached(Key key) {
        Value value = cache.get(key);
        return value == null ? null : new SimpleEntry<>(value == NULL ? null : value);
    }

    /**
     * Shortcut for ReadCache with byte[] keys. Also prevents accidental
     * usage of regular Map implementation (non byte[])
     */
    public static class BytesKey<V> extends ReadCache<byte[], V> implements CachedSource.BytesKey<V> {

        public BytesKey(Source<byte[], V> src) {
            super(src);
            withCache(new ByteArrayMap<V>());
        }

        public ReadCache.BytesKey<V> withMaxCapacity(int maxCapacity) {
            withCache(new ByteArrayMap<V>(new LRUMap<ByteArrayWrapper, V>(maxCapacity) {
                @Override
                protected boolean removeLRU(LinkEntry<ByteArrayWrapper, V> entry) {
                    cacheRemoved(entry.getKey().getData(), entry.getValue());
                    return super.removeLRU(entry);
                }
            }));
            return this;
        }
    }
}
