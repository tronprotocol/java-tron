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

import java.util.Comparator;

/**
 * Created by kest on 5/26/15.
 */
public class TimeComparator implements Comparator<NodeEntry> {

  @Override
  public int compare(NodeEntry e1, NodeEntry e2) {
    long t1 = e1.getModified();
    long t2 = e2.getModified();

    if (t1 < t2) {
      return 1;
    } else if (t1 > t2) {
      return -1;
    } else {
      return 0;
    }
  }
}
