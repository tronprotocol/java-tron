package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.AssetIssueCapsule;

public class AssetIssueStore extends TronStoreWithRevoking<AssetIssueCapsule> {

  private static final Logger logger = LoggerFactory.getLogger("AssetIssueStore");
  private static AssetIssueStore instance;


  private AssetIssueStore(String dbName) {
    super(dbName);
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
