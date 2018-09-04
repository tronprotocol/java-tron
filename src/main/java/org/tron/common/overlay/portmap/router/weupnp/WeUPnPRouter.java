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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.PortMappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.portmap.model.PortMapping;
import org.tron.common.overlay.portmap.model.Protocol;
import org.tron.common.overlay.portmap.router.AbstractRouter;
import org.tron.common.overlay.portmap.router.IRouter;
import org.tron.common.overlay.portmap.router.RouterException;

/**
 * This class is an implements an {@link IRouter} using the weupnp library's {@link GatewayDevice}.
 */
public class WeUPnPRouter extends AbstractRouter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final GatewayDevice device;

  WeUPnPRouter(final GatewayDevice device) {
    super(device.getFriendlyName());
    this.device = device;
  }

  @Override
  public void addPortMapping(final PortMapping mapping) throws RouterException {
    try {
      device.addPortMapping(mapping.getExternalPort(), mapping.getInternalPort(),
          mapping.getInternalClient(),
          mapping.getProtocol().getName(), mapping.getDescription());
    } catch (final Exception e) {
      throw new RouterException("Could not add portmapping", e);
    }
  }

  @Override
  public void addPortMappings(final Collection<PortMapping> mappings) throws RouterException {
    for (final PortMapping mapping : mappings) {
      this.addPortMapping(mapping);
    }
  }

  @Override
  public void disconnect() {
    // noting to do right now
  }

  @Override
  public String getExternalIPAddress() throws RouterException {
    try {
      return device.getExternalIPAddress();
    } catch (final Exception e) {
      throw new RouterException("Could not get external IP address", e);
    }
  }

  @Override
  public String getInternalHostName() {
    final String url = device.getPresentationURL();
    if (url == null || url.trim().length() == 0) {
      return null;
    }
    try {
      return new URL(url).getHost();
    } catch (final MalformedURLException e) {
      logger.warn("Could not get URL for internal host name '" + url + "'", e);
      return url;
    }
  }

  @Override
  public int getInternalPort() throws RouterException {
    String url = device.getPresentationURL();
    if (url == null) {
      url = device.getURLBase();
      logger.info("Presentation url is null: use url base '{}'", url);
    }
    if (url == null) {
      throw new RouterException("Presentation URL and URL base are null");
    }

    try {
      return new URL(url).getPort();
    } catch (final MalformedURLException e) {
      throw new RouterException("Could not get internal port from URL '" + url + "'", e);
    }
  }

  @Override
  public Collection<PortMapping> getPortMappings() throws RouterException {
    final Collection<PortMapping> mappings = new LinkedList<>();
    boolean morePortMappings = true;
    int index = 0;
    while (morePortMappings) {
      final PortMappingEntry entry = new PortMappingEntry();
      try {
        logger.debug("Getting port mapping {}...", index);
        if (!device.getGenericPortMappingEntry(index, entry)) {
          logger.debug("Entry {} does not exist, stop getting more mappings", index);
          morePortMappings = false;
        } else {
          logger.debug("Got port mapping {}: {}", index, entry);
        }
      } catch (final Exception e) {
        morePortMappings = false;
        logger.debug("Got an exception with message '{}' for index {}, stop getting more mappings",
            e.getMessage(), index);
        logger.trace("Exception", e);
      }

      if (entry.getProtocol() != null) {
        final Protocol protocol =
            entry.getProtocol().equalsIgnoreCase("TCP") ? Protocol.TCP : Protocol.UDP;
        final PortMapping m = new PortMapping(protocol, entry.getRemoteHost(),
            entry.getExternalPort(),
            entry.getInternalClient(), entry.getInternalPort(), entry.getPortMappingDescription());
        mappings.add(m);
      } else {
        logger.debug("Got null port mapping for index {}", index);
      }
      index++;
    }
    return mappings;
  }

  @Override
  public void logRouterInfo() throws RouterException {
    final Map<String, String> info = new HashMap<>();
    info.put("friendly name", device.getFriendlyName());
    info.put("manufacturer", device.getManufacturer());
    info.put("model description", device.getModelDescription());
    info.put("location", device.getLocation());
    info.put("device type", device.getDeviceType());
    info.put("device type cif", device.getDeviceTypeCIF());

    final SortedSet<String> sortedKeys = new TreeSet<>(info.keySet());

    logger.info("Router info:");
    for (final String key : sortedKeys) {
      final String value = info.get(key);
      logger.info("- {} = {}", key, value);
    }
  }

  @Override
  public void removeMapping(final PortMapping mapping) throws RouterException {
    this.removePortMapping(mapping.getProtocol(), mapping.getRemoteHost(),
        mapping.getExternalPort());
  }

  @Override
  public void removePortMapping(final Protocol protocol, final String remoteHost,
      final int externalPort)
      throws RouterException {
    try {
      device.deletePortMapping(externalPort, protocol.getName());
    } catch (final Exception e) {
      throw new RouterException("Could not delete port mapping", e);
    }
  }
}
