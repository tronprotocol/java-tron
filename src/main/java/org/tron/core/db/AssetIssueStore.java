package org.tron.core.db;

import static org.tron.core.config.Parameter.DatabaseConstants.ASSET_ISSUE_COUNT_LIMIT_MAX;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;

@Slf4j
@Component
public class AssetIssueStore extends TronStoreWithRevoking<AssetIssueCapsule> {

  @Autowired
  private AssetIssueStore(@Value("asset-issue") String dbName) {
    super(dbName);
  }


  @Override
  public AssetIssueCapsule get(byte[] key) {
    return super.getUnchecked(key);
  }

  /**
   * get all asset issues.
   */
  public List<AssetIssueCapsule> getAllAssetIssues() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  public List<AssetIssueCapsule> getAssetIssuesPaginated(long offset, long limit) {
    if (limit < 0 || offset < 0) {
      return null;
    }

//    return Streams.stream(iterator())
//        .map(Entry::getValue)
//        .sorted(Comparator.comparing(a -> a.getName().toStringUtf8(), String::compareTo))
//        .skip(offset)
//        .limit(Math.min(limit, ASSET_ISSUE_COUNT_LIMIT_MAX))
//        .collect(Collectors.toList());

    List<AssetIssueCapsule> assetIssueList = getAllAssetIssues();
    if (assetIssueList.size() <= offset) {
      return null;
    }
    assetIssueList.sort((o1, o2) -> {
      if (o1.getName() != o2.getName()) {
        return o1.getName().toStringUtf8().compareTo(o2.getName().toStringUtf8());
      }
      return Long.compare(o1.getOrder(), o2.getOrder());
    });
    limit = limit > ASSET_ISSUE_COUNT_LIMIT_MAX ? ASSET_ISSUE_COUNT_LIMIT_MAX : limit;
    long end = offset + limit;
    end = end > assetIssueList.size() ? assetIssueList.size() : end ;
    return assetIssueList.subList((int)offset,(int)end);
  }

}
