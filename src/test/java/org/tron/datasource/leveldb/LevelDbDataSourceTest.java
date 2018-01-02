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
package org.tron.datasource.leveldb;

import org.junit.Ignore;
import org.junit.Test;
import org.tron.utils.ByteArray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public class LevelDbDataSourceTest {

    @Test
    public void testGet()  {
        LevelDbDataSource dataSource = new LevelDbDataSource("test");
        dataSource.init();
        String key1="000134yh";
        byte[] key = key1.getBytes();
        byte[] value = dataSource.get(key);
        String s = ByteArray.toStr(value);
        dataSource.close();
        System.out.println(s);
    }

    @Test
    public void testPut() {
        LevelDbDataSource dataSource = new LevelDbDataSource("test");
        dataSource.init();
        String key1="000134yh";
        byte[] key = key1.getBytes();

        String value1="50000";
        byte[] value = value1.getBytes();

        dataSource.put(key,value);

        assertNotNull(dataSource.get(key));
        assertEquals(1, dataSource.keys().size());

        dataSource.close();
    }

    @Test
    public void testRest() {
        LevelDbDataSource dataSource = new LevelDbDataSource("test");
        dataSource.reset();
        dataSource.close();
    }

}