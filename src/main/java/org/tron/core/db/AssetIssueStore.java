package org.tron.core.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.db.common.iterator.AssetIssueIterator;

@Slf4j
@Component
public class AssetIssueStore extends TronStoreWithRevoking<AssetIssueCapsule> {

  @Autowired
  private AssetIssueStore(@Value("asset-issue") String dbName) {
    super(dbName);
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
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      indexHelper.update(item.getInstance());
    }
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
  public Iterator<Entry<byte[], AssetIssueCapsule>> iterator() {
    return new AssetIssueIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      AssetIssueCapsule item = get(key);
      if (Objects.nonNull(item)) {
        indexHelper.remove(item.getInstance());
      }
    }
  }
}
