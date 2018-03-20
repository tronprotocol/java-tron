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


import java.net.InetSocketAddress;

public class DiscoveryEvent {

    private org.ethereum.net.rlpx.DiscoverMessage message;
    private InetSocketAddress address;

    public DiscoveryEvent(org.ethereum.net.rlpx.DiscoverMessage m, InetSocketAddress a) {
        message = m;
        address = a;
    }

    public org.ethereum.net.rlpx.DiscoverMessage getMessage() {
        return message;
    }

    public void setMessage(org.ethereum.net.rlpx.DiscoverMessage message) {
        this.message = message;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }
}
