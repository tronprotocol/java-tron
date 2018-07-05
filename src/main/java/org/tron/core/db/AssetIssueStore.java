package org.tron.core.db;

import static org.tron.core.config.Parameter.DatabaseConstants.ASSET_ISSUE_COUNT_LIMIT_MAX;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

  public List<AssetIssueCapsule> getAssetIssuesPaginated(long offset, long limit) {
    if (limit < 0 || offset < 0) {
      return null;
    }
    List<AssetIssueCapsule> assetIssueList = dbSource.allKeys().stream()
        .map(this::get)
        .collect(Collectors.toList());
    if (assetIssueList.size() <= offset) {
      return null;
    }
    assetIssueList.sort((o1, o2) -> {
      return o1.getName().toStringUtf8().compareTo(o2.getName().toStringUtf8());
    });
    limit = limit > ASSET_ISSUE_COUNT_LIMIT_MAX ? ASSET_ISSUE_COUNT_LIMIT_MAX : limit;
    long end = offset + limit;
    end = end > assetIssueList.size() ? assetIssueList.size() : end ;
    return assetIssueList.subList((int)offset,(int)end);
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
