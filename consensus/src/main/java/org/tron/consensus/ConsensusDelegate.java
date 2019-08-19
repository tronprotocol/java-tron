package org.tron.consensus;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.core.store.WitnessStore;

@Slf4j(topic = "consensus")
@Component
public class ConsensusDelegate {

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private AccountStore accountStore;

  @Autowired
  private WitnessStore witnessStore;

  @Autowired
  private WitnessScheduleStore witnessScheduleStore;

  @Autowired
  private VotesStore votesStore;

  public int calculateFilledSlotsCount() {
    return dynamicPropertiesStore.calculateFilledSlotsCount();
  }

  public void saveRemoveThePowerOfTheGr(long rate) {
    dynamicPropertiesStore.saveRemoveThePowerOfTheGr(rate);
  }

  public long getRemoveThePowerOfTheGr() {
    return dynamicPropertiesStore.getRemoveThePowerOfTheGr();
  }

  public long getWitnessStandbyAllowance() {
    return  dynamicPropertiesStore.getWitnessStandbyAllowance();
  }

  public long getLatestBlockHeaderTimestamp() {
    return dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
  }

  public long getLatestBlockHeaderNumber() {
    return dynamicPropertiesStore.getLatestBlockHeaderNumber();
  }

  public boolean lastHeadBlockIsMaintenance() {
    return dynamicPropertiesStore.getStateFlag() == 1;
  }

  public long getMaintenanceSkipSlots() {
    return dynamicPropertiesStore.getMaintenanceSkipSlots();
  }

  public WitnessCapsule getWitnesseByAddress(ByteString address) {
    return witnessStore.get(address.toByteArray());
  }

  public void saveActiveWitnesses(List<ByteString> addresses) {
    witnessScheduleStore.saveActiveWitnesses(addresses);
  }

  public List<ByteString> getActiveWitnesses() {
    return witnessScheduleStore.getActiveWitnesses();
  }

  public WitnessStore getWitnessStore() {
    return witnessStore;
  }

  public VotesStore getVotesStore() {
    return votesStore;
  }

  public AccountStore getAccountStore() {
    return accountStore;
  }

  public void saveStateFlag(int flag) {
    dynamicPropertiesStore.saveStateFlag(flag);
  }

  public int saveStateFlag() {
    return dynamicPropertiesStore.getStateFlag();
  }

  public long getMaintenanceTimeInterval() {
    return dynamicPropertiesStore.getMaintenanceTimeInterval();
  }

  public void saveMaintenanceTimeInterval(long time) {
    dynamicPropertiesStore.saveMaintenanceTimeInterval(time);
  }

  public long getNextMaintenanceTime() {
    return dynamicPropertiesStore.getNextMaintenanceTime();
  }

  public void saveNextMaintenanceTime(long time) {
    dynamicPropertiesStore.saveNextMaintenanceTime(time);
  }

  public long getWitnessPayPerBlock() {
    return dynamicPropertiesStore.getWitnessPayPerBlock();
  }

  public long getLatestSolidifiedBlockNum() {
    return dynamicPropertiesStore.getLatestSolidifiedBlockNum();
  }

  public void saveLatestSolidifiedBlockNum(long num) {
    dynamicPropertiesStore.saveLatestSolidifiedBlockNum(num);
  }

  public int[] getBlockFilledSlots() {
    return dynamicPropertiesStore.getBlockFilledSlots();
  }

  public void saveBlockFilledSlots(int[] slots) {
    dynamicPropertiesStore.saveBlockFilledSlots(slots);
  }

  public int getBlockFilledSlotsIndex() {
    return dynamicPropertiesStore.getBlockFilledSlotsIndex();
  }

  public void saveBlockFilledSlotsIndex(int index) {
    dynamicPropertiesStore.saveBlockFilledSlotsIndex(index);
  }

  public int getBlockFilledSlotsNumber() {
    return dynamicPropertiesStore.getBlockFilledSlotsNumber();
  }

  public Sha256Hash getLatestBlockHeaderHash() {
    return dynamicPropertiesStore.getLatestBlockHeaderHash();
  }

  public long getAllowMultiSign() {
    return dynamicPropertiesStore.getAllowMultiSign();
  }

}