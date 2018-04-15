package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.AssetIssueCapsule;

@Slf4j
public class AssetIssueStore extends TronStoreWithRevoking<AssetIssueCapsule> {

  private AssetIssueStore(String dbName) {
    super(dbName);
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AssetIssueStore create(String dbName) {
    return new AssetIssueStore(dbName);
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
