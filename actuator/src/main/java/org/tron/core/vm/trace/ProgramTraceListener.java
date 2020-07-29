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
package org.tron.core.vm.trace;

import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.program.listener.ProgramListenerAdaptor;

public class ProgramTraceListener extends ProgramListenerAdaptor {

  private final boolean enabled;
  private OpActions actions = new OpActions();

  public ProgramTraceListener(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void onMemoryExtend(int delta) {
    if (enabled) {
      actions.addMemoryExtend(delta);
    }
  }

  @Override
  public void onMemoryWrite(int address, byte[] data, int size) {
    if (enabled) {
      actions.addMemoryWrite(address, data, size);
    }
  }

  @Override
  public void onStackPop() {
    if (enabled) {
      actions.addStackPop();
    }
  }

  @Override
  public void onStackPush(DataWord value) {
    if (enabled) {
      actions.addStackPush(value);
    }
  }

  @Override
  public void onStackSwap(int from, int to) {
    if (enabled) {
      actions.addStackSwap(from, to);
    }
  }

  @Override
  public void onStoragePut(DataWord key, DataWord value) {
    if (enabled) {
      if (value.equals(DataWord.ZERO)) {
        actions.addStorageRemove(key);
      } else {
        actions.addStoragePut(key, value);
      }
    }
  }

  @Override
  public void onStorageClear() {
    if (enabled) {
      actions.addStorageClear();
    }
  }

  public OpActions resetActions() {
    OpActions actions = this.actions;
    this.actions = new OpActions();
    return actions;
  }
}
