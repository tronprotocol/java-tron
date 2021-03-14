package org.tron.core.db.api;

import java.util.Iterator;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;

@Slf4j(topic = "DB")
public class VotePowerUpdateHelper {

  private ChainBaseManager chainBaseManager;

  public VotePowerUpdateHelper(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start to update the vote power");
    updateAccount();
    finish();
    logger.info(
        "Complete the vote power update, Total time:{} milliseconds",
        System.currentTimeMillis() - start);
  }


  public void updateAccount() {
    long count = 0;

    Iterator<Entry<byte[], AccountCapsule>> iterator =
        chainBaseManager.getAccountStore().iterator();
    while (iterator.hasNext()) {
      AccountCapsule accountCapsule = iterator.next().getValue();
      accountCapsule.setVotePower12(accountCapsule.getTronPower());
      chainBaseManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

      if (count % 50000 == 0) {
        logger.info("The number of accounts that have completed the update: {}", count);
      }
      count++;
    }

    logger.info("Complete the account store update, total assets: {}", count);
  }

  public void finish() {
    chainBaseManager.getDynamicPropertiesStore().saveVotePowerUpdateDone(1);
  }
}
