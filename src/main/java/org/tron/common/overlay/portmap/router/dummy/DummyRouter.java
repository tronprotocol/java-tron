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
package org.tron.common.overlay.portmap.router.dummy;

import java.util.Collection;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.portmap.model.PortMapping;
import org.tron.common.overlay.portmap.model.Protocol;
import org.tron.common.overlay.portmap.router.AbstractRouter;
import org.tron.common.overlay.portmap.router.RouterException;

public class DummyRouter extends AbstractRouter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Collection<PortMapping> mappings;

  public DummyRouter(final String name) {
    super(name);
    logger.debug("Created new DummyRouter");
    mappings = new LinkedList<>();
    mappings.add(new PortMapping(Protocol.TCP, "remoteHost1", 1, "internalClient1", 1,
        getName() + ": dummy port mapping 1"));
    mappings.add(
        new PortMapping(Protocol.UDP, null, 2, "internalClient2", 2,
            getName() + ": dummy port mapping 2"));
    mappings.add(
        new PortMapping(Protocol.TCP, null, 3, "internalClient3", 3,
            getName() + ": dummy port mapping 3"));
  }

  @Override
  public void addPortMapping(final PortMapping mapping) {
    logger.debug("Adding mapping " + mapping);
    mappings.add(mapping);
  }

  @Override
  public void addPortMappings(final Collection<PortMapping> mappingsToAdd) {
    logger.debug("Adding {} mappings: {}", mappingsToAdd.size(), mappingsToAdd);
    this.mappings.addAll(mappingsToAdd);
  }

  @Override
  public void disconnect() {
    logger.debug("Disconnect");
  }

  @Override
  public String getExternalIPAddress() {
    return "DummyExternalIP";
  }

  @Override
  public String getInternalHostName() {
    return "DummyInternalHostName";
  }

  @Override
  public int getInternalPort() {
    return 42;
  }

  @Override
  public Collection<PortMapping> getPortMappings() {
    try {
      logger.debug("Sleep 3s to simulate delay when fetching port mappings.");
      Thread.sleep(3000);
    } catch (final InterruptedException e) {
      // ignore
      Thread.currentThread().interrupt();
    }
    return new LinkedList<>(mappings);
  }

  @Override
  public void logRouterInfo() {
    logger.info("DummyRouter " + getName());
  }

  @Override
  public void removeMapping(final PortMapping mapping) {
    mappings.remove(mapping);
  }

  @Override
  public void removePortMapping(final Protocol protocol, final String remoteHost,
      final int externalPort) {
    // ignore
  }

  @Override
  public String getLocalHostAddress() throws RouterException {
    return "DummyLocalhostAddress";
  }
}
