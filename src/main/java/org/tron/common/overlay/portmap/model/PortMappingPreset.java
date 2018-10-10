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
package org.tron.common.overlay.portmap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.tron.common.overlay.portmap.Settings;

/**
 * This class stores a port mapping preset containing a description, internal and remote host and a
 * {@link List} of {@link SinglePortMapping}s.
 */
public class PortMappingPreset implements Cloneable, Serializable {

  private static final long serialVersionUID = 3749136884938395765L;

  /**
   * The description of this preset.
   */
  private String description;

  /**
   * The ip address of the internal client or <code>null</code> for localhost.
   */
  private String internalClient;

  /**
   * The host name of the remote host.
   */
  private String remoteHost;

  /**
   * The port mappings in this preset.
   */
  private List<SinglePortMapping> ports;

  /**
   * <code>true</code> if this preset has not been saved.
   */
  private boolean isNew;

  /**
   * Creates a new preset with the given default values.
   */
  public PortMappingPreset(final String remoteHost, final String internalClient,
      final String description) {
    this.remoteHost = remoteHost;
    this.internalClient = internalClient;
    this.description = description;
    this.ports = new LinkedList<>();

    this.isNew = false;
  }

  /**
   * Creates a new empty preset.
   */
  public PortMappingPreset() {
    this.ports = new LinkedList<>();
    this.isNew = true;
  }

  @Override
  public String toString() {
    return description;
  }

  public List<PortMapping> getPortMappings(final String localhost) {
    if (this.useLocalhostAsInternalClient() && (localhost == null || localhost.length() == 0)) {
      throw new IllegalArgumentException("Got invalid localhost and internal host is not given.");
    }

    final List<PortMapping> allPortMappings = new ArrayList<>(this.ports.size());
    for (final SinglePortMapping port : this.ports) {
      final String internalClientName =
          this.useLocalhostAsInternalClient() ? localhost : this.internalClient;

      final PortMapping newMapping = new PortMapping(port.getProtocol(), remoteHost,
          port.getExternalPort(),
          internalClientName, port.getInternalPort(), description);

      allPortMappings.add(newMapping);
    }

    return allPortMappings;
  }

  public String getCompleteDescription() {
    final StringBuffer b = new StringBuffer();

    b.append(" ");
    b.append(remoteHost);
    b.append(":");

    b.append(" -> ");
    b.append(internalClient);
    b.append(":");

    b.append(" ");

    b.append(" ");
    b.append(description);
    return b.toString();
  }

  public List<SinglePortMapping> getPorts() {
    return new LinkedList<>(ports);
  }

  public void setPorts(final List<SinglePortMapping> ports) {
    this.ports = new LinkedList<>(ports);
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setRemoteHost(final String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public String getRemoteHost() {
    return remoteHost;
  }

  public void setInternalClient(final String internalClient) {
    this.internalClient = internalClient;
  }

  public String getInternalClient() {
    return internalClient;
  }

  public boolean isNew() {
    return isNew;
  }

  public void setNew(final boolean isNew) {
    this.isNew = isNew;
  }

  public boolean useLocalhostAsInternalClient() {
    return this.getInternalClient() == null || this.getInternalClient().length() == 0;
  }

  public void save(final Settings settings) {
    if (this.isNew) {
      settings.addPreset(this);
    } else {
      settings.savePreset(this);
    }
    this.isNew = false;
  }
}
