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
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.tron.common.overlay.portmap.model.PortMapping;

public class DeletePortMappingAction extends AbstractClingAction<Void> {

  private final int externalPort;
  private final String protocol;
  private final String remoteHost;

  public DeletePortMappingAction(final RemoteService service, final PortMapping portMapping) {
    super(service, "DeletePortMapping");
    this.externalPort = portMapping.getExternalPort();
    this.protocol = portMapping.getProtocol().getName();
    this.remoteHost = portMapping.getRemoteHost();
  }

  @Override
  public Map<String, Object> getArgumentValues() {
    final HashMap<String, Object> args = new HashMap<>();
    args.put("NewExternalPort", new UnsignedIntegerTwoBytes(externalPort));
    args.put("NewProtocol", protocol);
    args.put("NewRemoteHost", remoteHost);
    return args;
  }

  @Override
  public Void convert(final ActionInvocation<RemoteService> response) {
    return null;
  }
}
