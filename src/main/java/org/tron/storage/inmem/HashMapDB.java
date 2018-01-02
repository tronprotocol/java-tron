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
package org.tron.storage.inmem;

import org.tron.storage.DbSourceInter;
import org.tron.utils.ALock;
import org.tron.utils.ByteArrayMap;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class HashMapDB<V> implements DbSourceInter<V> {

    protected final Map<byte[], V> storage;

    protected ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected ALock readLock = new ALock(rwLock.readLock());
    protected ALock writeLock = new ALock(rwLock.writeLock());

    public HashMapDB() {
        this(new ByteArrayMap<V>());
    }

    public HashMapDB(ByteArrayMap<V> storage) {
        this.storage = storage;
    }

    @Override
    public void putData(byte[] key, V val) {
        if (val == null) {
            deleteData(key);
        } else {
            try (ALock l = writeLock.lock()) {
                storage.put(key, val);
            }
        }
    }

    @Override
    public V getData(byte[] key) {
        try (ALock l = readLock.lock()) {
            return storage.get(key);
        }
    }

    @Override
    public void deleteData(byte[] key) {
        try (ALock l = writeLock.lock()) {
            storage.remove(key);
        }
    }

    @Override
    public boolean flush() {
        return true;
    }

    @Override
    public void setDBName(String name) {}

    @Override
    public String getDBName() {
        return "in-memory";
    }

    @Override
    public void initDB() {}

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void closeDB() {}

    @Override
    public Set<byte[]> allKeys() {
        try (ALock l = readLock.lock()) {
            return getStorage().keySet();
        }
    }

    @Override
    public void updateByBatch(Map<byte[], V> rows) {
        try (ALock l = writeLock.lock()) {
            for (Map.Entry<byte[], V> entry : rows.entrySet()) {
                putData(entry.getKey(), entry.getValue());
            }
        }
    }

    public Map<byte[], V> getStorage() {
        return storage;
    }
}
