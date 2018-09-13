/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.server;

import org.tron.common.overlay.message.Message;

/**
 * Utility wraps around a message to keep track of the number of times it has been offered This
 * class also contains the last time a message was offered and is updated when an answer has been
 * received to it can be removed from the queue.
 *
 * @author Roman Mandeleil
 */
public class MessageRoundtrip {

  private final Message msg;
  long lastTimestamp = 0;
  long retryTimes = 0;
  boolean answered = false;

  public MessageRoundtrip(Message msg) {
    this.msg = msg;
    saveTime();
  }

  public boolean isAnswered() {
    return answered;
  }

  public void answer() {
    answered = true;
  }

  public long getRetryTimes() {
    return retryTimes;
  }

  public void incRetryTimes() {
    ++retryTimes;
  }

  public void saveTime() {
    lastTimestamp = System.currentTimeMillis();
  }

  public long getTime() {
    return lastTimestamp;
  }

  public boolean hasToRetry() {
    return 20000 < System.currentTimeMillis() - lastTimestamp;
  }

  public Message getMsg() {
    return msg;
  }
}
