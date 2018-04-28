package org.tron.core.db;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.db.common.iterator.AssetIssueIterator;

@Slf4j
@Component
public class AssetIssueStore extends TronStoreWithRevoking<AssetIssueCapsule> {

  private static AssetIssueStore instance;

  @Autowired
  private AssetIssueStore(@Qualifier("asset-issue") String dbName) {
    super(dbName);
  }

  public static void destroy() {
    instance = null;
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

  @Override
  public void put(byte[] key, AssetIssueCapsule item) {
    if (indexHelper != null) {
      indexHelper.update(item.getInstance());
    }
    super.put(key, item);
  }

  /**
   * get all asset issues.
   */
  public List<AssetIssueCapsule> getAllAssetIssues() {
    return dbSource.allKeys().stream()
        .map(this::get)
        .collect(Collectors.toList());
  }

  @Override
  public Iterator<AssetIssueCapsule> iterator() {
    return new AssetIssueIterator(dbSource.iterator());
  }

}
