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

package org.tron.core.services.interfaceOnSolidity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;

import java.util.concurrent.Callable;

@Slf4j(topic = "API")
@Component
public class WalletOnSolidity {
  @Autowired
  private Manager dbManager;

  public <T> T futureGet(TronCallable<T> callable) {
    try {
      dbManager.setMode(false);
      return callable.call();
    } finally {
      dbManager.setMode(true);
    }
  }

  public void futureGet(Runnable runnable) {
    try {
      dbManager.setMode(false);
      runnable.run();
    } finally {
      dbManager.setMode(true);
    }
  }

  public interface TronCallable<T> extends Callable<T> {
    @Override
    T call();
  }
}
