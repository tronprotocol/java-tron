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
package org.tron.common.overlay.portmap.router;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base class for all router factories.
 */
public abstract class AbstractRouterFactory {

    private static final String LOCATION_URL_SYSTEM_PROPERTY = "portmapper.locationUrl";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String name;

    protected AbstractRouterFactory(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the router factory that can be displayed to the user.
     *
     * @return the name of the router factory that can be displayed to the user.
     */
    public String getName() {
        return name;
    }

    public List<IRouter> findRouters() throws RouterException {
        final String locationUrl = System.getProperty(LOCATION_URL_SYSTEM_PROPERTY);
        if (locationUrl == null) {
            logger.debug("System property '{}' not defined: discover routers automatically.",
                    LOCATION_URL_SYSTEM_PROPERTY);
            return findRoutersInternal();
        }
        logger.info("Trying to connect using location url {}", locationUrl);
        return asList(connect(locationUrl));
    }

    /**
     * Search for routers on the network.
     *
     * @return the found router or an empty {@link Collection} if no router was found.
     * @throws RouterException
     *             if something goes wrong during discovery.
     */
    protected abstract List<IRouter> findRoutersInternal() throws RouterException;

    /**
     * Directly connect to a router using a location url like <code>http://192.168.179.1:49000/igddesc.xml</code>.
     *
     * @param locationUrl
     *            a location url
     * @return a router if the connection was successful.
     * @throws RouterException
     *             if something goes wrong during connection.
     */
    protected abstract IRouter connect(final String locationUrl) throws RouterException;

    @Override
    public String toString() {
        return name;
    }

    public void shutdownService() {

    }
}
