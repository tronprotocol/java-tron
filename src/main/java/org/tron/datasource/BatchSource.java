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

import java.util.Map;

/**
 * The Source which is capable of batch updates.
 * The semantics of a batch update is up to implementation:
 * it can be just performance optimization or batch update
 * can be atomic or other.
 */
public interface BatchSource<K, V> extends Source<K, V> {

    /**
     * Do batch update
     *
     * @param rows Normally this Map is treated just as a collection
     *             of key-value pairs and shouldn't conform to a normal
     *             Map contract. Though it is up to implementation to
     *             require passing specific Maps
     */
    void updateBatch(Map<K, V> rows);
}
