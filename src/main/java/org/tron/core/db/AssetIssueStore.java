package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AssetIssueCapsule;

public class AssetIssueStore extends TronDatabase<AssetIssueCapsule> {

  private static final Logger logger = LoggerFactory.getLogger("AssetIssueStore");
  private static AssetIssueStore instance;


  private AssetIssueStore(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, AssetIssueCapsule item) {
    logger.info("asset issue is {}, asset issue is {}", key, item);

    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(value)) {
      onModify(key, value);
    }

    logger.info("name is {} ", ByteArray.toHexString(key));
    dbSource.putData(key, item.getData());

    if (ArrayUtils.isEmpty(value)) {
      onCreate(key);
    }
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AssetIssueStore create(String dbName) {
    if (instance == null) {
      synchronized (AssetIssueStore.class) {
        if (instance == null) {
          instance = new AssetIssueStore(dbName);
        }
      }
    }
    return instance;
  }

  @Override
  public void delete(byte[] key) {
    // This should be called just before an object is removed.
    onDelete(key);
    dbSource.deleteData(key);
  }

  @Override
  public AssetIssueCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new AssetIssueCapsule(value);
  }

  /**
   * isAssetIssusExist fun.
   *
   * @param key the address of Account
   */
  @Override
  public boolean has(byte[] key) {
    byte[] assetIssue = dbSource.getData(key);
    logger.info("name is {}, asset issue is {}", key, assetIssue);
    return null != assetIssue;
  }

  /**
   * get all asset issues.
   */
  public List<AssetIssueCapsule> getAllAssetIssues() {
    return dbSource.allKeys().stream()
        .map(this::get)
        .collect(Collectors.toList());
  }
}
