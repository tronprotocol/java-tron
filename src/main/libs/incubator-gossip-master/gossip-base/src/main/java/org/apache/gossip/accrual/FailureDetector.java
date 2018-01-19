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

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

public class FailureDetector {

  public static final Logger LOGGER = Logger.getLogger(FailureDetector.class);
  private final DescriptiveStatistics descriptiveStatistics;
  private final long minimumSamples;
  private volatile long latestHeartbeatMs = -1;
  private final String distribution;

  public FailureDetector(long minimumSamples, int windowSize, String distribution) {
    descriptiveStatistics = new DescriptiveStatistics(windowSize);
    this.minimumSamples = minimumSamples;
    this.distribution = distribution;
  }

  /**
   * Updates the statistics based on the delta between the last
   * heartbeat and supplied time
   *
   * @param now the time of the heartbeat in milliseconds
   */
  public synchronized void recordHeartbeat(long now) {
    if (now <= latestHeartbeatMs) {
      return;
    }
    if (latestHeartbeatMs != -1) {
      descriptiveStatistics.addValue(now - latestHeartbeatMs);
    }
    latestHeartbeatMs = now;
  }

  public synchronized Double computePhiMeasure(long now) {
    if (latestHeartbeatMs == -1 || descriptiveStatistics.getN() < minimumSamples) {
      return null;
    }
    long delta = now - latestHeartbeatMs;
    try {
      double probability;
      if (distribution.equals("normal")) {
        double standardDeviation = descriptiveStatistics.getStandardDeviation();
        standardDeviation = standardDeviation < 0.1 ? 0.1 : standardDeviation;
        probability = new NormalDistributionImpl(descriptiveStatistics.getMean(), standardDeviation).cumulativeProbability(delta);
      } else {
        probability = new ExponentialDistributionImpl(descriptiveStatistics.getMean()).cumulativeProbability(delta);
      }
      final double eps = 1e-12;
      if (1 - probability < eps) {
        probability = 1.0;
      }
      return -1.0d * Math.log10(1.0d - probability);
    } catch (MathException | IllegalArgumentException e) {
      LOGGER.debug(e);
      return null;
    }
  }
}
