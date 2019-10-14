package org.tron.consensus.pbft;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.Param;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.store.WitnessScheduleStore;

@Slf4j(topic = "pbft")
@Component
public class PbftMessageHandle {

  public static final int TIME_OUT = 60000;
  //Pre-preparation stage voting information
  private Set<String> preVotes = Sets.newConcurrentHashSet();
  //Preparation stage voting information
  private Set<String> pareVotes = Sets.newConcurrentHashSet();
  private AtomicLongMap<String> agreePare = AtomicLongMap.create();
  private Cache<String, PbftBaseMessage> pareMsgCache = CacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();
  //Submit stage voting information
  private Set<String> commitVotes = Sets.newConcurrentHashSet();
  private AtomicLongMap<String> agreeCommit = AtomicLongMap.create();
  private Cache<String, PbftBaseMessage> commitMsgCache = CacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();
  //pbft timeout
  private Map<String, Long> timeOuts = Maps.newConcurrentMap();
  //Successfully processed request
  private Map<String, PbftBaseMessage> doneMsg = Maps.newConcurrentMap();

  private Timer timer;

  @Autowired
  private PbftMessageAction pbftMessageAction;
  @Autowired
  private ApplicationContext ctx;
  @Autowired
  private WitnessScheduleStore witnessScheduleStore;
  @Autowired
  private ChainBaseManager chainBaseManager;
  @Setter
  private MaintenanceManager maintenanceManager;

  @PostConstruct
  public void init() {
    start();
  }

  public void onPrePrepare(PbftBaseMessage message) {
    String key = message.getNo();
    if (message.isSwitch()) {//if is block chain switch,remove the before proposal
      logger.warn("block chain switch, again proposal block num: {}, data: {}",
          message.getBlockNum(), message.getDataString());
      remove(key);
    }
    if (preVotes.contains(key)) {
      //The description has been initiated, can not be repeated, can only initiate a vote at the same height
      return;
    }
    preVotes.add(key);
    //Start timeout control
    timeOuts.put(key, System.currentTimeMillis());
    //
    checkPrepareMsgCache(key);
    //Into the preparation phase, if not the sr node does not need to be prepared
    if (!checkIsCanSendMsg(message)) {
      return;
    }Args.java
    PbftBaseMessage paMessage = message.buildPrePareMessage();
    forwardMessage(paMessage);
  }

  public void onPrepare(PbftBaseMessage message) {
    String key = message.getKey();

    if (!preVotes.contains(message.getNo())) {
      //Must be prepared in advance
      pareMsgCache.put(key, message);
      return;
    }
    if (pareVotes.contains(key)) {
      //Explain that the vote has been voted and cannot be repeated
      return;
    }
    pareVotes.add(key);
    //
    checkCommitMsgCache(message.getNo());
    if (!checkIsCanSendMsg(message)) {
      return;
    }
    //The number of votes plus 1
    if (!doneMsg.containsKey(message.getNo())) {
      long agCou = agreePare.incrementAndGet(message.getDataKey());
      if (agCou >= Param.getInstance().getAgreeNodeCount()) {
        agreePare.remove(message.getDataKey());
        //Entering the submission stage
        PbftBaseMessage cmMessage = message.buildCommitMessage();
        doneMsg.put(message.getNo(), cmMessage);
        forwardMessage(cmMessage);
      }
    }
    //Subsequent votes will definitely not be satisfied, timeout will be automatically cleared.
  }

  public void onCommit(PbftBaseMessage message) {
    String key = message.getKey();
    if (!pareVotes.contains(key)) {
      //Must be prepared
      commitMsgCache.put(key, message);
      return;
    }
    if (commitVotes.contains(key)) {
      //Explain that the node has voted on the data and cannot vote repeatedly.
      return;
    }
    commitVotes.add(key);
    //The number of votes plus 1
    long agCou = agreeCommit.incrementAndGet(message.getDataKey());
    if (agCou >= Param.getInstance().getAgreeNodeCount()) {
      remove(message.getNo());
      //commit,
      if (!isSyncing()) {
        pbftMessageAction.action(message);
      }
    }
  }

  public void onRequestData(PbftBaseMessage message) {

  }

  public void onChangeView(PbftBaseMessage message) {

  }

  public void forwardMessage(PbftBaseMessage message) {
    Param.getInstance().getPbftInterface().forwardMessage(message);
  }

  private void checkPrepareMsgCache(String key) {
    for (Entry<String, PbftBaseMessage> entry : pareMsgCache.asMap().entrySet()) {
      if (StringUtils.startsWith(entry.getKey(), key)) {
        pareMsgCache.invalidate(entry.getKey());
        onPrepare(entry.getValue());
      }
    }
  }

  private void checkCommitMsgCache(String key) {
    for (Entry<String, PbftBaseMessage> entry : commitMsgCache.asMap().entrySet()) {
      if (StringUtils.startsWith(entry.getKey(), key)) {
        commitMsgCache.invalidate(entry.getKey());
        onCommit(entry.getValue());
      }
    }
  }

  public boolean checkIsCanSendMsg(PbftBaseMessage msg) {
    if (!Param.getInstance().isEnable()) {
      return false;
    }
    if (!witnessScheduleStore.getActiveWitnesses().stream()
        .anyMatch(witness -> Arrays.equals(witness.toByteArray(),
            Param.getInstance().getMiner().getPrivateKeyAddress().toByteArray()))) {
      return false;
    }
    return !isSyncing();
  }

  public boolean checkIsWitnessMsg(PbftBaseMessage msg) {
    //check current node is witness node
    if (maintenanceManager == null) {
      return false;
    }
    long blockNum = msg.getPbftMessage().getRawData().getBlockNum();
    List<ByteString> witnessList;
    BlockCapsule blockCapsule = null;
    try {
      blockCapsule = Param.getInstance().getPbftInterface().getBlock(blockNum);
    } catch (Exception e) {
      logger.debug("can not find the block,num is: {}, error reason: {}", blockNum, e.getMessage());
    }
    if (blockCapsule == null || blockCapsule.getTimeStamp() > maintenanceManager
        .getBeforeMaintenanceTime()) {
      witnessList = maintenanceManager.getCurrentWitness();
    } else {
      witnessList = maintenanceManager.getBeforeWitness();
    }
    return witnessList.stream()
        .anyMatch(witness -> witness.equals(msg.getPbftMessage().getRawData().getPublicKey()));
  }

  public boolean isSyncing() {
    return Param.getInstance().getPbftInterface().isSyncing();
  }

  //Cleanup related status
  private void remove(String no) {
    String pre = String.valueOf(no) + "_";
    preVotes.remove(no);
    pareVotes.removeIf((vp) -> StringUtils.startsWith(vp, pre));
    commitVotes.removeIf((vp) -> StringUtils.startsWith(vp, pre));

    agreePare.asMap().keySet().forEach(s -> {
      if (StringUtils.startsWith(s, pre)) {
        long value = agreePare.remove(s);
        logger.debug("{} agreePare count:{}", no, value);
      }
    });
    agreeCommit.asMap().keySet().forEach(s -> {
      if (StringUtils.startsWith(s, pre)) {
        long value = agreeCommit.remove(s);
        logger.debug("{} agreeCommit count:{}", no, value);
      }
    });
    doneMsg.remove(no);
    timeOuts.remove(no);
  }

  /**
   * Detect timeout
   */
  private void checkTimer() {
    List<String> remo = Lists.newArrayList();
    for (Entry<String, Long> item : timeOuts.entrySet()) {
      if (System.currentTimeMillis() - item.getValue() > TIME_OUT) {
        //If the timeout has not been agreed, the vote will be invalid.
        logger.info("vote will be invalid:{}", item.getKey());
        remove(item.getKey());
      }
    }
  }

  public void start() {
    timer = new Timer("pbft-timer");
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        checkTimer();
      }
    }, 10, 1000);
  }
}
