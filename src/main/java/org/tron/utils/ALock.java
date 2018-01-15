/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.utils;

import java.util.concurrent.locks.Lock;

/**
 * AutoClosable Lock wrapper. Use case:
 * <p>
 * try (ALock l = wLock.lock()) {
 * // do smth under lock
 * }
 */
public final class ALock implements AutoCloseable {
  private final Lock lock;

  public ALock(Lock l) {
    this.lock = l;
  }

  public final ALock lock() {
    this.lock.lock();
    return this;
  }

  public final void close() {
    this.lock.unlock();
  }
}
