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
package org.tron.common.overlay.portmap.router;

import java.util.Collection;
import org.tron.common.overlay.portmap.model.PortMapping;
import org.tron.common.overlay.portmap.model.Protocol;

public interface IRouter extends AutoCloseable {

  public abstract String getName();

  /**
   * Get the IP address of the local host.
   *
   * @return IP address of the local host or <code>null</code>, if the address could not be
   * determined.
   */
  public String getLocalHostAddress() throws RouterException;

  /**
   * Get the external IP of the router.
   *
   * @return the external IP of the router.
   */
  public abstract String getExternalIPAddress() throws RouterException;

  /**
   * Get the internal host name or IP of the router.
   *
   * @return the internal host name or IP of the router.
   */
  public abstract String getInternalHostName();

  /**
   * Get the internal port of the router.
   *
   * @return the internal port of the router.
   */
  public abstract int getInternalPort() throws RouterException;

  /**
   * Get all port mappings from the router.
   *
   * @return all port mappings from the router.
   * @throws RouterException if something went wrong when getting the port mappings.
   */
  public abstract Collection<PortMapping> getPortMappings() throws RouterException;

  /**
   * Write information about the router to the log.
   */
  public abstract void logRouterInfo() throws RouterException;

  /**
   * Add the given port mappings to the router.
   *
   * @param mappings the port mappings to add.
   */
  public abstract void addPortMappings(Collection<PortMapping> mappings) throws RouterException;

  /**
   * Add the given port mapping to the router.
   *
   * @param mapping the port mapping to add.
   */
  public abstract void addPortMapping(PortMapping mapping) throws RouterException;

  /**
   * Remove the given port mapping from the router.
   *
   * @param mapping the port mapping to remove.
   */
  public abstract void removeMapping(PortMapping mapping) throws RouterException;

  /**
   * Remove the port mapping with the given data from the router.
   */
  public abstract void removePortMapping(Protocol protocol, String remoteHost, int externalPort)
      throws RouterException;

  /**
   * Disconnect from the router.
   */
  public abstract void disconnect();

  @Override
  void close();
}