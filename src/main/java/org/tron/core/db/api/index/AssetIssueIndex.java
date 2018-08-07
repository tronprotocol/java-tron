package org.tron.core.db.api.index;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.disk.DiskIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.protos.Contract.AssetIssueContract;

import javax.annotation.PostConstruct;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

@Component
@Slf4j
public class AssetIssueIndex extends AbstractIndex<AssetIssueCapsule, AssetIssueContract> {

  public static Attribute<WrappedByteArray, String> AssetIssue_OWNER_ADDRESS;
  public static SimpleAttribute<WrappedByteArray, String> AssetIssue_NAME;
  public static Attribute<WrappedByteArray, Long> AssetIssue_START;
  public static Attribute<WrappedByteArray, Long> AssetIssue_END;

  @Autowired
  public AssetIssueIndex(
      @Qualifier("assetIssueStore") final ITronChainBase<AssetIssueCapsule> database) {
    super(database);
  }

  @PostConstruct
  public void init() {
    initIndex(DiskPersistence.onPrimaryKeyInFile(AssetIssue_NAME, indexPath));
    index.addIndex(DiskIndex.onAttribute(AssetIssue_OWNER_ADDRESS));
//    index.addIndex(DiskIndex.onAttribute(AssetIssue_NAME));
    index.addIndex(DiskIndex.onAttribute(AssetIssue_START));
    index.addIndex(DiskIndex.onAttribute(AssetIssue_END));
  }

  @Override
  protected void setAttribute() {
    AssetIssue_OWNER_ADDRESS =
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
