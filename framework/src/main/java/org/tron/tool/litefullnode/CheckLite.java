package org.tron.tool.litefullnode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.rocksdb.RocksDBException;
import org.tron.tool.litefullnode.db.TronDB;
import org.tron.tool.litefullnode.iterator.DBIterator;

@Slf4j(topic = "tool")
public class CheckLite implements Callable<Boolean> {
  
  private final String name;
  private final String fullDir;
  private final String liteDir;

  public CheckLite(String fullDir, String liteDir, String name) {
    this.fullDir = fullDir;
    this.liteDir = liteDir;
    this.name = name;
  }

  @Override
  public Boolean call() throws Exception {
    if (LiteFullNodeTool.BLOCK_DB_NAME.equalsIgnoreCase(name)
        || LiteFullNodeTool.BLOCK_INDEX_DB_NAME.equalsIgnoreCase(name)
        || LiteFullNodeTool.CHECKPOINT_DB.equalsIgnoreCase(name)
        || LiteFullNodeTool.PROPERTIES_DB_NAME.equalsIgnoreCase(name)
    ) {
      return checkKV();
    }
    return checkMd5();
  }

  private Boolean checkKV() throws IOException, RocksDBException {
    TronDB full = DbTool.getDB(fullDir, name);
    TronDB lite = DbTool.getDB(liteDir, name);

    DBIterator liteIterator =  lite.iterator();

    for (liteIterator.seekToFirst(); liteIterator.isValid(); liteIterator.next()) {
      byte[] key = liteIterator.getKey();
      byte[] value = liteIterator.getValue();
      Assert.assertArrayEquals("db: [" + name + "], value check ",
          full.getDir(key), value);
    }

    liteIterator.close();

    TronDB.removeDB(Paths.get(fullDir, name).toString()).close();
    TronDB.removeDB(Paths.get(liteDir, name).toString()).close();
    logger.info("DB [{}] check kv pair passed", name);
    return true;
  }

  private Boolean checkMd5() {
    Path fullPath = Paths.get(fullDir, name);
    Path litePath = Paths.get(liteDir, name);
    Arrays.stream(Objects.requireNonNull(litePath.toFile().listFiles()))
        .filter(File::isFile).map(File::getName).parallel().forEach(file -> {
          Path fullFile = Paths.get(fullPath.toString(), file);
          Path liteFile = Paths.get(litePath.toString(), file);
          try (InputStream isFull = new FileInputStream(fullFile.toFile());
               InputStream isLite = new FileInputStream(liteFile.toFile())) {
            Assert.assertEquals(liteFile.toString(),
                DigestUtils.md5Hex(isFull), DigestUtils.md5Hex(isLite));
          } catch (IOException e) {
            Assert.fail(e.getMessage());
          }
        });
    logger.info("DB [{}] check md5 passed", this.name);
    return true;
  }

}
