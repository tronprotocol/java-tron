/**
 * UPnP PortMapper - A tool for managing port forwardings via UPnP Copyright (C) 2015 Christoph
 * Pirkl <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 */
package org.tron.common.overlay.portmap.router.weupnp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.portmap.router.AbstractRouterFactory;
import org.tron.common.overlay.portmap.router.IRouter;
import org.tron.common.overlay.portmap.router.RouterException;

/**
 * A router factoring using the weupnp library.
 */
public class WeUPnPRouterFactory extends AbstractRouterFactory {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final GatewayDiscover discover = new GatewayDiscover();

  public WeUPnPRouterFactory() {
    super("weupnp lib");
  }

  @Override
  protected List<IRouter> findRoutersInternal() throws RouterException {
    logger.debug("Searching for gateway devices...");
    final Map<InetAddress, GatewayDevice> devices;
    try {
      devices = discover.discover();
    } catch (final Exception e) {
      throw new RouterException("Could not discover a valid gateway device: " + e.getMessage(), e);
    }

    if (devices == null || devices.size() == 0) {
      return Collections.emptyList();
    }

    final List<IRouter> routers = new ArrayList<>(devices.size());
    for (final GatewayDevice device : devices.values()) {
      routers.add(new WeUPnPRouter(device));
    }
    return routers;
  }

  @Override
  protected IRouter connect(final String locationUrl) throws RouterException {

    final GatewayDevice device = new GatewayDevice();
    device.setLocation(locationUrl);
    device.setSt("urn:schemas-upnp-org:device:InternetGatewayDevice:1");
    try {
      device.setLocalAddress(InetAddress.getLocalHost());
    } catch (final UnknownHostException e) {
      throw new RouterException("Could not get ip of localhost: " + e.getMessage(), e);
    }
    try {
      device.loadDescription();
    } catch (final Exception e) {
      throw new RouterException(
          "Could not load description of device for location url " + locationUrl + " : " + e
              .getMessage(), e);
    }
    return new WeUPnPRouter(device);
  }
}
