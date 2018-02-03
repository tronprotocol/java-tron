/*
 * Copyright (c) 2015 Cryptonomex, Inc., and contributors.
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.tron.core.witness;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.util.List;
import lombok.Getter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.core.net.message.BlockMessage;
import org.tron.protos.Protocal;

public class Witness {

  private static final Logger logger = LoggerFactory.getLogger(Witness.class);
  private static final int LOOP_INTERVAL = 1000;

  private Application tApp;
  @Getter
  protected WitnessCapsule localWitnessState; //  WitnessId;
  @Getter
  protected List<WitnessCapsule> witnessStates;

  private Thread GenerateThread;
  private Manager db;

  private volatile boolean isRunning = false;

  public Witness(Application tApp) {
    this.tApp = tApp;
    db = tApp.getDbManager();
    GenerateThread = new Thread(scheduleProductionLoop);
    init();
  }

  protected void init() {
    localWitnessState = new WitnessCapsule(ByteString.copyFromUtf8("0x11"));
    this.witnessStates = db.getWitnesses();
  }

  public void startUp() {
    isRunning = true;
    GenerateThread.start();
  }

  public void stop() {
    isRunning = false;
    GenerateThread.interrupt();
  }

  private Runnable scheduleProductionLoop =
      () -> {
        while (isRunning) {
          DateTime time = DateTime.now();
          int timeToNextSecond = LOOP_INTERVAL - time.getMillisOfSecond();
          if (timeToNextSecond < 50) {
            timeToNextSecond = timeToNextSecond + LOOP_INTERVAL;
          }
          try {
            DateTime nextTime = time.plus(timeToNextSecond);
            logger.info("sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
            Thread.sleep(timeToNextSecond);
            blockProductionLoop();
          } catch (Exception ex) {
            logger.error("ProductionLoop error", ex);
          }
        }
      };

  private void blockProductionLoop() {
    String result = null;
    String capture = null;
    try {
      result = tryProduceBlock(capture);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    // todo
    switch (result) {
      default:
    }
  }


  private String tryProduceBlock(String capture) {

    long slot = getSlotAtTime(DateTime.now());
    if (slot == 0) {
      // todo capture error message
      return "";
    }

    ByteString scheduled_witness = db.getScheduledWitness(slot);
    logger.info("scheduled_witness:" + scheduled_witness + ",slot:" + slot);

    //            if( !witnesses.contains( scheduled_witness ) == _witnesses.end() ){
    if (!scheduled_witness.equals(getLocalWitnessState().getClass())) {

      logger.info("not tune");
      return "";
    }
    // todo add other verification
    DateTime scheduledTime = getSlotTime(slot);
    // todo verify校验
    Protocal.Block block = generateBlock(scheduledTime);
    logger.info("generate block successfully");
    broadcastBlock(block);
    return "";
  }

  protected void broadcastBlock(Protocal.Block block) {
    try {
      tApp.getP2pNode().broadcast(new BlockMessage(block));
    } catch (Exception ex) {
      throw new RuntimeException("broadcastBlock error");
    }
    logger.info("broadcast block successfully");
  }

  protected Protocal.Block generateBlock(DateTime when) {
    return tApp.getDbManager().generateBlock(localWitnessState, when.getMillis());
  }

  // shuffle todo


  protected long getCurrentSlot() {
    return 0; // todo
  }


  private DateTime getSlotTime(long slot_num) {
    if (slot_num == 0) {
      return DateTime.now();
    }
    long interval = blockInterval();

    // todo add other verification and need revised later
    //            long head_block_abs_slot = head_block_time().sec_since_epoch() / interval;
    //            DateTime head_slot_time = head_block_abs_slot * interval;

    DateTime head_slot_time = head_block_time();
    head_slot_time.plus(interval * slot_num);
    return head_slot_time;
  }

  private DateTime head_block_time() {
    return DateTime.now(); // todo
  }

  private long getSlotAtTime(DateTime when) {
    DateTime first_slot_time = getFirstSlotTime();
    //    if (when.isAfter(first_slot_time)) return 0;
    return (when.getMillis() - first_slot_time.getMillis()) / blockInterval() + 1;
  }

  protected DateTime getFirstSlotTime() {
    return DateTime.now(); // todo
  }

  private long blockInterval() {
    return LOOP_INTERVAL; // millisecond todo getFromDb
  }

  public static void main(String[] args) throws Exception {
    Application tApp = null;

    Witness witness =
        new Witness(tApp) {

          private long currentSlot = 5;

          private DateTime firstSlotTime = DateTime.now().minus(5000);

          @Override
          protected void init() {
            this.localWitnessState = new WitnessCapsule(ByteString.copyFromUtf8("0x12"));
            this.witnessStates = Lists.newArrayList();

            WitnessCapsule aState = new WitnessCapsule(ByteString.copyFromUtf8("0x13"));
            WitnessCapsule bState = new WitnessCapsule(ByteString.copyFromUtf8("0x14"));
            WitnessCapsule cState = new WitnessCapsule(ByteString.copyFromUtf8("0x15"));
            this.witnessStates.add(aState);
            this.witnessStates.add(bState);
            this.witnessStates.add(cState);
          }

          @Override
          protected Protocal.Block generateBlock(DateTime when) {
            return Protocal.Block.getDefaultInstance();
          }

          @Override
          protected void broadcastBlock(Protocal.Block block) {
            logger.info("broadcast block successfully");
          }

          @Override
          protected long getCurrentSlot() {
            return currentSlot++;
          }

          @Override
          protected DateTime getFirstSlotTime() {
            return firstSlotTime;
          }
        };
    witness.startUp();
    Thread.sleep(20000);
    witness.stop();
  }
}
