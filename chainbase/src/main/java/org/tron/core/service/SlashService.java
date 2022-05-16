package org.tron.core.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;

@Slf4j(topic = "slash")
@Component
public class SlashService {
  @Setter
  private WitnessStore witnessStore;

  @Setter
  private DynamicPropertiesStore dynamicPropertiesStore;

  public void initStore(WitnessStore witnessStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.witnessStore = witnessStore;
    this.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public void slashWitness(byte[] witnessAddress, long slashCount, boolean toJailed) {
    if (dynamicPropertiesStore.allowSlashVote()) {
      WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
      if (witnessCapsule.getVoteCount() > slashCount) {
        witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() - slashCount);
      } else {
        witnessCapsule.setVoteCount(0);
      }
      if (toJailed) {
        witnessCapsule.setJailedHeight(dynamicPropertiesStore.getLatestBlockHeaderNumber());
      }
      witnessStore.put(witnessAddress, witnessCapsule);
    }
  }
}
