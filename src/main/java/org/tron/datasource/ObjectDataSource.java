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


public class ObjectDataSource<V> extends SourceChainBox<byte[], V, byte[], byte[]> {

    ReadCache<byte[], V> cache;
    SourceCodec<byte[], V, byte[], byte[]> codec;
    Source<byte[], byte[]> byteSource;

    public ObjectDataSource(Source<byte[], byte[]> byteSource, Serializer<V, byte[]> serializer, int readCacheEntries) {
        super(byteSource);
        this.byteSource = byteSource;
        add(codec = new SourceCodec<>(byteSource, new Serializers.Identity<byte[]>(), serializer));
        if (readCacheEntries > 0) {
            add(cache = new ReadCache.BytesKey<>(codec).withMaxCapacity(readCacheEntries));
        }
    }
}
