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
package org.tron.common.overlay.discover.table;

/**
 * Created by kest on 5/25/15.
 */
public class KademliaOptions {

  public static final int BUCKET_SIZE = 16;
  public static final int ALPHA = 3;
  public static final int BINS = 256;
  public static final int MAX_STEPS = 8;

  public static final long REQ_TIMEOUT = 300;
  public static final long BUCKET_REFRESH = 7200;     //bucket refreshing interval in millis
  public static final long DISCOVER_CYCLE = 30;       //discovery cycle interval in seconds
}
