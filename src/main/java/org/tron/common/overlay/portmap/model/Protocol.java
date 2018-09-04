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

/**
 * This {@link Enum} represents the protocol of a {@link SinglePortMapping}, possible values are {@link #TCP} and
 * {@link #UDP}.
 */
public enum Protocol {

    TCP("TCP"), UDP("UDP");

    private final String name;

    private Protocol(final String name) {
        this.name = name;
    }

    public static Protocol getProtocol(final String name) {
        if (name != null && name.equalsIgnoreCase("TCP")) {
            return TCP;
        }
        if (name != null && name.equalsIgnoreCase("UDP")) {
            return UDP;
        }
        throw new IllegalArgumentException("Invalid protocol name '" + name + "'");
    }

    public String getName() {
        return name;
    }
}