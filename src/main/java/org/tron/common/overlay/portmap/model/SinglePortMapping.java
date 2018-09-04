/**
 * UPnP PortMapper - A tool for managing port forwardings via UPnP
 * Copyright (C) 2015 Christoph Pirkl <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 */
package org.tron.common.overlay.portmap.model;

import java.io.Serializable;

/**
 * This class is used by {@link PortMapping} to store the information about a single port mapping, i.e. the protocol
 * (TCP or UDP) and internal and extern port.
 */
public class SinglePortMapping implements Cloneable, Serializable {

    private static final long serialVersionUID = 7458514232916039775L;
    private int externalPort;
    private int internalPort;
    private Protocol protocol;

    public SinglePortMapping() {
        this(Protocol.TCP, 1, 1);
    }

    public SinglePortMapping(final Protocol protocol, final int internalPort, final int externalPort) {
        this.protocol = protocol;
        this.internalPort = internalPort;
        this.externalPort = externalPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public void setExternalPort(final int externalPort) {
        this.externalPort = externalPort;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(final Protocol protocol) {
        this.protocol = protocol;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public void setInternalPort(final int internalPort) {
        this.internalPort = internalPort;
    }

    @Override
    public Object clone() {
        return new SinglePortMapping(protocol, internalPort, externalPort);
    }
}
