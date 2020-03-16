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

import java.util.HashMap;
import java.util.Map;
import org.tron.common.runtime.vm.DataWord;

public class ProgramStorageChangeListener extends ProgramListenerAdaptor {

  private Map<DataWord, DataWord> diff = new HashMap<>();

  @Override
  public void onStoragePut(DataWord key, DataWord value) {
    diff.put(key, value);
  }

  @Override
  public void onStorageClear() {
    // do nothing
  }

  public Map<DataWord, DataWord> getDiff() {
    return new HashMap<>(diff);
  }

  public void merge(Map<DataWord, DataWord> diff) {
    this.diff.putAll(diff);
  }
}
