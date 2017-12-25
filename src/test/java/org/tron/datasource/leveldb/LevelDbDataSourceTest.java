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
        String key1="000134yyyh";
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
        String key1="000134yyyh";
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