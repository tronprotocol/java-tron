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
package org.tron.common.overlay.portmap.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the abstract super class for all routers.
 */
public abstract class AbstractRouter implements IRouter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String name;

    public AbstractRouter(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the the ip of the local host.
     */
    @Override
    public String getLocalHostAddress() throws RouterException {
        logger.debug("Get IP of localhost");

        final InetAddress localHostIP = getLocalHostAddressFromSocket();

        // We do not want an address like 127.0.0.1
        if (localHostIP.getHostAddress().startsWith("127.")) {
            throw new RouterException("Only found an address that begins with '127.' when retrieving IP of localhost");
        }

        return localHostIP.getHostAddress();
    }

    /**
     * Get the ip of the local host by connecting to the router and fetching the ip from the socket. This only works
     * when we are connected to the router and know its internal upnp port.
     *
     * @return the ip of the local host.
     * @throws RouterException
     */
    private InetAddress getLocalHostAddressFromSocket() throws RouterException {
        InetAddress localHostIP = null;
        try {
            // In order to use the socket method to get the address, we have to
            // be connected to the router.
            final int routerInternalPort = getInternalPort();
            logger.debug("Got internal router port {}", routerInternalPort);

            // Check, if we got a correct port number
            if (routerInternalPort > 0) {
                logger.debug("Creating socket to router: {}:{}...", getInternalHostName(), routerInternalPort);
                try (Socket socket = new Socket(getInternalHostName(), routerInternalPort)) {
                    localHostIP = socket.getLocalAddress();
                } catch (final UnknownHostException e) {
                    throw new RouterException(
                            "Could not create socked to " + getInternalHostName() + ":" + routerInternalPort, e);
                }

                logger.debug("Got address {} from socket.", localHostIP);
            } else {
                logger.debug("Got invalid internal router port number {}", routerInternalPort);
            }

            // We are not connected to the router or got an invalid port number,
            // so we have to use the traditional method.
            if (localHostIP == null) {

                logger.debug(
                        "Not connected to router or got invalid port number, can not use socket to determine the address of the localhost. "
                                + "If no address is found, please connect to the router.");

                localHostIP = InetAddress.getLocalHost();

                logger.debug("Got address {} via InetAddress.getLocalHost().", localHostIP);
            }

        } catch (final IOException e) {
            throw new RouterException("Could not get IP of localhost.", e);
        }
        return localHostIP;
    }

    @Override
    public void close() {
        this.disconnect();
    }

    @Override
    public String toString() {
        return getName() + " (" + getInternalHostName() + ")";
    }
}
