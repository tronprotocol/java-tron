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

package org.tron.common.overlay.discover;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeManager;

@Slf4j(topic = "discover")
public class RefreshTask extends DiscoverTask {

  public RefreshTask(NodeManager nodeManager) {
    super(nodeManager);
  }

  @Override
  public void run() {
    discover(Node.getNodeId(), 0, new ArrayList<>());
  }
}
