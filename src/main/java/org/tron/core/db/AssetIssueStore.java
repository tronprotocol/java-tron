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

  /**
   * get all asset issues.
   */
  public List<AssetIssueCapsule> getAllAssetIssues() {
    return dbSource.allKeys().stream()
        .map(this::getUnchecked)
        .collect(Collectors.toList());
  }

  public List<AssetIssueCapsule> getAssetIssuesPaginated(long offset, long limit) {
    if (limit < 0 || offset < 0) {
      return null;
    }
    List<AssetIssueCapsule> assetIssueList = dbSource.allKeys().stream()
        .map(this::getUnchecked)
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

}
