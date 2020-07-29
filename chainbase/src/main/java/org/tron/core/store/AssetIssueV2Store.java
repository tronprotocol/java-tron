package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j(topic = "DB")
@Component
public class AssetIssueV2Store extends AssetIssueStore {

  @Autowired
  private AssetIssueV2Store(@Value("asset-issue-v2") String dbName) {
    super(dbName);
  }

}
