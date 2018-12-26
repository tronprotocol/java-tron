package org.tron.core.db.fast.historydata;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.DYNAMIC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum;
import org.tron.core.db.fast.callback.FastSyncCallBack;

@Component
public class HistoryDataPackage {

  @Autowired
  private Manager manager;

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  public void oneTime() {
    DynamicPropertiesStore dynamicPropertiesStore = manager.getDynamicPropertiesStore();
    dynamicPropertiesStore.iterator().forEachRemaining(entry -> {
      fastSyncCallBack.callBack(entry.getKey(), entry.getValue().getData(), DYNAMIC);
    });

  }

}
