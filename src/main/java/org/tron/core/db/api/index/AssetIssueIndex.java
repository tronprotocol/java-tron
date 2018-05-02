package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.protos.Contract.AssetIssueContract;

@Component
@Slf4j
public class AssetIssueIndex extends AbstractIndex<AssetIssueCapsule, AssetIssueContract> {

  public static Attribute<WrappedByteArray, String> AssetIssue_OWNER_RADDRESS;
  public static Attribute<WrappedByteArray, String> AssetIssue_NAME;
  public static Attribute<WrappedByteArray, Long> AssetIssue_START;
  public static Attribute<WrappedByteArray, Long> AssetIssue_END;

  @Autowired
  public AssetIssueIndex(
      @Qualifier("assetIssueStore") final TronDatabase<AssetIssueCapsule> database) {
    super();
    this.database = database;
  }

  public AssetIssueIndex(
      final TronDatabase<AssetIssueCapsule> database,
      Persistence<WrappedByteArray, ? extends Comparable> persistence) {
    super(persistence);
    this.database = database;
  }

  @PostConstruct
  public void init() {
    index.addIndex(SuffixTreeIndex.onAttribute(AssetIssue_OWNER_RADDRESS));
    index.addIndex(SuffixTreeIndex.onAttribute(AssetIssue_NAME));
    index.addIndex(NavigableIndex.onAttribute(AssetIssue_START));
    index.addIndex(NavigableIndex.onAttribute(AssetIssue_END));
    fill();
  }

  @Override
  protected void setAttribute() {
    AssetIssue_OWNER_RADDRESS =
        attribute(
            "assetIssue owner address",
            bytes -> {
              AssetIssueContract assetIssue = getObject(bytes);
              return ByteArray.toHexString(assetIssue.getOwnerAddress().toByteArray());
            });

    AssetIssue_NAME =
        attribute("assetIssue name", bytes -> {
          AssetIssueContract assetIssue = getObject(bytes);
          return assetIssue.getName().toStringUtf8();
        });

    AssetIssue_START =
        attribute("assetIssue start time", bytes -> {
          AssetIssueContract assetIssue = getObject(bytes);
          return assetIssue.getStartTime();
        });

    AssetIssue_END =
        attribute("assetIssue end time", bytes -> {
          AssetIssueContract assetIssue = getObject(bytes);
          return assetIssue.getEndTime();
        });

  }
}
