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

import java.util.HashMap;
import java.util.Map;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.tron.common.overlay.portmap.model.PortMapping;

public class AddPortMappingAction extends AbstractClingAction<Void> {

  private final PortMapping portMapping;

  public AddPortMappingAction(final Service<RemoteDevice, RemoteService> service,
      final PortMapping portMapping) {
    super(service, "AddPortMapping");
    this.portMapping = portMapping;
  }

  @Override
  public Map<String, Object> getArgumentValues() {
    final HashMap<String, Object> args = new HashMap<>();
    args.put("NewExternalPort", new UnsignedIntegerTwoBytes(portMapping.getExternalPort()));
    args.put("NewProtocol", portMapping.getProtocol());
    args.put("NewInternalClient", portMapping.getInternalClient());
    args.put("NewInternalPort", new UnsignedIntegerTwoBytes(portMapping.getInternalPort()));
    args.put("NewLeaseDuration", new UnsignedIntegerFourBytes(portMapping.getLeaseDuration()));
    args.put("NewEnabled", portMapping.isEnabled());
    args.put("NewRemoteHost", portMapping.getRemoteHost());
    args.put("NewPortMappingDescription", portMapping.getDescription());
    return args;
  }

  @Override
  public Void convert(final ActionInvocation<RemoteService> response) {
    return null;
  }
}
