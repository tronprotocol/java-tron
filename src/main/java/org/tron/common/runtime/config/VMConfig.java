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
package org.tron.common.runtime.config;

/**
 * For developer only
 */
public class VMConfig {

  public static final int MAX_CODE_LENGTH = 1024 * 1024;

  public static final int MAX_FEE_LIMIT = 1_000_000_000; //1000 trx

  private boolean vmTraceCompressed = false;
  private boolean vmTrace = false;


  private VMConfig() {
  }

  private static class SystemPropertiesInstance {

    private static final VMConfig INSTANCE = new VMConfig();
  }

  public static VMConfig getInstance() {
    return SystemPropertiesInstance.INSTANCE;
  }

  public boolean vmTrace() {
    return vmTrace;
  }

  public boolean vmTraceCompressed() {
    return vmTraceCompressed;
  }


}
