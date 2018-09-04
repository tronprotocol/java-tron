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

/**
 * This immutable class represents a port mapping / forwarding on a router.
 */
public class PortMapping implements Cloneable {

  public static final String MAPPING_ENTRY_LEASE_DURATION = "NewLeaseDuration";
  public static final String MAPPING_ENTRY_ENABLED = "NewEnabled";
  public static final String MAPPING_ENTRY_REMOTE_HOST = "NewRemoteHost";
  public static final String MAPPING_ENTRY_INTERNAL_CLIENT = "NewInternalClient";
  public static final String MAPPING_ENTRY_PORT_MAPPING_DESCRIPTION = "NewPortMappingDescription";
  public static final String MAPPING_ENTRY_PROTOCOL = "NewProtocol";
  public static final String MAPPING_ENTRY_INTERNAL_PORT = "NewInternalPort";
  public static final String MAPPING_ENTRY_EXTERNAL_PORT = "NewExternalPort";

  private static final long DEFAULT_LEASE_DURATION = 0;

  private final int externalPort;
  private final Protocol protocol;
  private final int internalPort;
  private final String description;
  private final String internalClient;
  private final String remoteHost;
  private final boolean enabled;
  private final long leaseDuration;

  public PortMapping(final Protocol protocol, final String remoteHost, final int externalPort,
      final String internalClient, final int internalPort, final String description) {
    this(protocol, remoteHost, externalPort, internalClient, internalPort, description, true,
        DEFAULT_LEASE_DURATION);
  }

  public PortMapping(final Protocol protocol, final String remoteHost, final int externalPort,
      final String internalClient, final int internalPort, final String description,
      final boolean enabled,
      final long leaseDuration) {
    this.protocol = protocol;
    this.remoteHost = remoteHost;
    this.externalPort = externalPort;
    this.internalClient = internalClient;
    this.internalPort = internalPort;
    this.description = description;
    this.enabled = enabled;
    this.leaseDuration = leaseDuration;
  }

//    private PortMapping(final ActionResponse response) {
//        final Map<String, String> values = new HashMap<>();
//
//        for (final Object argObj : response.getOutActionArgumentNames()) {
//            final String argName = (String) argObj;
//            values.put(argName, response.getOutActionArgumentValue(argName));
//        }
//
//        externalPort = Integer.parseInt(values.get(MAPPING_ENTRY_EXTERNAL_PORT));
//        internalPort = Integer.parseInt(values.get(MAPPING_ENTRY_INTERNAL_PORT));
//        final String protocolString = values.get(MAPPING_ENTRY_PROTOCOL);
//        protocol = (protocolString.equalsIgnoreCase("TCP") ? Protocol.TCP : Protocol.UDP);
//        description = values.get(MAPPING_ENTRY_PORT_MAPPING_DESCRIPTION);
//        internalClient = values.get(MAPPING_ENTRY_INTERNAL_CLIENT);
//        remoteHost = values.get(MAPPING_ENTRY_REMOTE_HOST);
//        final String enabledString = values.get(MAPPING_ENTRY_ENABLED);
//        enabled = enabledString != null && enabledString.equals("1");
//        leaseDuration = Long.parseLong(values.get(MAPPING_ENTRY_LEASE_DURATION));
//    }
//
//    public static PortMapping create(final ActionResponse response) {
//        final PortMapping mapping = new PortMapping(response);
//        return mapping;
//    }

  /**
   * @return the leaseDuration
   */
  public long getLeaseDuration() {
    return leaseDuration;
  }

  public int getExternalPort() {
    return externalPort;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public int getInternalPort() {
    return internalPort;
  }

  public String getDescription() {
    return description;
  }

  public String getInternalClient() {
    return internalClient;
  }

  public String getRemoteHost() {
    return remoteHost;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getCompleteDescription() {
    final StringBuilder b = new StringBuilder();
    b.append(protocol);
    b.append(" ");
    if (remoteHost != null) {
      b.append(remoteHost);
    }
    b.append(":");
    b.append(externalPort);
    b.append(" -> ");
    b.append(internalClient);
    b.append(":");
    b.append(internalPort);
    b.append(" ");
    b.append(enabled ? "enabled" : "not enabled");
    b.append(" ");
    b.append(description);
    return b.toString();
  }

  @Override
  public String toString() {
    return description;
  }

  @Override
  public Object clone() {
    return new PortMapping(protocol, remoteHost, externalPort, internalClient, internalPort,
        description, enabled,
        leaseDuration);
  }
}
