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
package org.tron.core.vm.program.listener;

import org.tron.common.runtime.vm.DataWord;

// Empty ListenerAdapter
public class ProgramListenerAdaptor implements ProgramListener {

  @Override
  public void onMemoryExtend(int delta) {
    // do nothing
  }

  @Override
  public void onMemoryWrite(int address, byte[] data, int size) {
    // do nothing
  }

  @Override
  public void onStackPop() {
    // do nothing
  }

  @Override
  public void onStackPush(DataWord value) {
    // do nothing
  }

  @Override
  public void onStackSwap(int from, int to) {
    // do nothing
  }

  @Override
  public void onStoragePut(DataWord key, DataWord value) {
    // do nothing
  }

  @Override
  public void onStorageClear() {
    // do nothing
  }
}
