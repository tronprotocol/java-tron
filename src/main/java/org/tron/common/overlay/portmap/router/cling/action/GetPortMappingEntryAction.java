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
package org.tron.common.overlay.portmap.router.cling.action;

import java.util.Collections;
import java.util.Map;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.tron.common.overlay.portmap.model.PortMapping;
import org.tron.common.overlay.portmap.model.Protocol;

public class GetPortMappingEntryAction extends AbstractClingAction<PortMapping> {

  private final int index;

  public GetPortMappingEntryAction(final Service<RemoteDevice, RemoteService> service,
      final int index) {
    super(service, "GetGenericPortMappingEntry");
    this.index = index;
  }

  @Override
  public Map<String, Object> getArgumentValues() {
    return Collections.<String, Object>singletonMap("NewPortMappingIndex",
        new UnsignedIntegerTwoBytes(index));
  }

  @Override
  public PortMapping convert(final ActionInvocation<RemoteService> response) {
    final Protocol protocol = Protocol.getProtocol(getStringValue(response, "NewProtocol"));
    final String remoteHost = getStringValue(response, "NewRemoteHost");
    final int externalPort = getIntValue(response, "NewExternalPort");
    final String internalClient = getStringValue(response, "NewInternalClient");
    final int internalPort = getIntValue(response, "NewInternalPort");
    final String description = getStringValue(response, "NewPortMappingDescription");
    final boolean enabled = getBooleanValue(response, "NewEnabled");
    final long leaseDuration = getLongValue(response, "NewLeaseDuration");
    return new PortMapping(protocol, remoteHost, externalPort, internalClient, internalPort,
        description, enabled,
        leaseDuration);
  }

  private boolean getBooleanValue(final ActionInvocation<RemoteService> response,
      final String argumentName) {
    return (boolean) response.getOutput(argumentName).getValue();
  }

  protected int getIntValue(final ActionInvocation<?> response, final String argumentName) {
    return ((UnsignedIntegerTwoBytes) response.getOutput(argumentName).getValue()).getValue()
        .intValue();
  }

  protected long getLongValue(final ActionInvocation<?> response, final String argumentName) {
    return ((UnsignedIntegerFourBytes) response.getOutput(argumentName).getValue()).getValue()
        .longValue();
  }

  protected String getStringValue(final ActionInvocation<?> response, final String argumentName) {
    return (String) response.getOutput(argumentName).getValue();
  }
}
