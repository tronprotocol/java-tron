/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.accrual;

import org.apache.gossip.GossipSettings;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(JUnitPlatform.class)
public class FailureDetectorTest {

  @FunctionalInterface
  interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
  }

  static final Double failureThreshold = new GossipSettings().getConvictThreshold();

  List<Integer> generateTimeList(int begin, int end, int step) {
    List<Integer> values = new ArrayList<>();
    Random rand = new Random();
    for (int i = begin; i < end; i += step) {
      int delta = (int) ((rand.nextDouble() - 0.5) * step / 2);

      values.add(i + delta);
    }
    return values;
  }

  @Test
  public void normalDistribution() {
    FailureDetector fd = new FailureDetector(1, 1000, "normal");
    List<Integer> values = generateTimeList(0, 10000, 100);
    Double deltaSum = 0.0;
    Integer deltaCount = 0;
    for (int i = 0; i < values.size() - 1; i++) {
      fd.recordHeartbeat(values.get(i));
      if (i != 0) {
        deltaSum += values.get(i) - values.get(i - 1);
        deltaCount++;
      }
    }
    Integer lastRecorded = values.get(values.size() - 2);

    //after "step" delay we need to be considered UP
    Assert.assertTrue(fd.computePhiMeasure(values.get(values.size() - 1)) < failureThreshold);

    //if we check phi-measure after mean delay we get value for 0.5 probability(normal distribution)
    Assert.assertEquals(fd.computePhiMeasure(lastRecorded + Math.round(deltaSum / deltaCount)), -Math.log10(0.5), 0.1);
  }

  @Test
  public void checkMinimumSamples() {
    Integer minimumSamples = 5;
    FailureDetector fd = new FailureDetector(minimumSamples, 1000, "normal");
    for (int i = 0; i < minimumSamples + 1; i++) { // +1 because we don't place first heartbeat into structure
      Assert.assertNull(fd.computePhiMeasure(100));
      fd.recordHeartbeat(i);
    }
    Assert.assertNotNull(fd.computePhiMeasure(100));
  }

  @Test
  public void checkMonotonicDead() {
    final FailureDetector fd = new FailureDetector(5, 1000, "normal");
    TriConsumer<Integer, Integer, Integer> checkAlive = (begin, end, step) -> {
      List<Integer> times = generateTimeList(begin, end, step);
      for (int i = 0; i < times.size(); i++) {
        Double current = fd.computePhiMeasure(times.get(i));
        if (current != null) {
          Assert.assertTrue(current < failureThreshold);
        }
        fd.recordHeartbeat(times.get(i));
      }
    };

    TriConsumer<Integer, Integer, Integer> checkDeadMonotonic = (begin, end, step) -> {
      List<Integer> times = generateTimeList(begin, end, step);
      Double prev = null;
      for (int i = 0; i < times.size(); i++) {
        Double current = fd.computePhiMeasure(times.get(i));
        if (current != null && prev != null) {
          Assert.assertTrue(current >= prev);
        }
        prev = current;
      }
    };

    checkAlive.accept(0, 20000, 100);
    checkDeadMonotonic.accept(20000, 20500, 5);
  }
}
