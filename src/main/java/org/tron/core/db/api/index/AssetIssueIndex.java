package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.AssetIssueContract;

@Component
@Slf4j
public class AssetIssueIndex extends AbstractIndex<AssetIssueContract> {

  public static final Attribute<AssetIssueContract, String> AssetIssue_OWNER_RADDRESS =
      attribute(
          "assetIssue owner address",
          assetIssue -> ByteArray.toHexString(assetIssue.getOwnerAddress().toByteArray()));

  public static final Attribute<AssetIssueContract, String> AssetIssue_NAME =
      attribute("assetIssue name", assetIssue -> assetIssue.getName().toStringUtf8());

  public static final Attribute<AssetIssueContract, Long> AssetIssue_START =
      attribute("assetIssue start time", AssetIssueContract::getStartTime);

  public static final Attribute<AssetIssueContract, Long> AssetIssue_END =
      attribute("assetIssue end time", AssetIssueContract::getEndTime);

  public AssetIssueIndex() {
    super();
  }

  public AssetIssueIndex(Persistence<AssetIssueContract, ? extends Comparable> persistence) {
    super(persistence);
  }

  @PostConstruct
  public void init() {
    addIndex(SuffixTreeIndex.onAttribute(AssetIssue_OWNER_RADDRESS));
    addIndex(SuffixTreeIndex.onAttribute(AssetIssue_NAME));
    addIndex(NavigableIndex.onAttribute(AssetIssue_START));
    addIndex(NavigableIndex.onAttribute(AssetIssue_END));
  }
}
