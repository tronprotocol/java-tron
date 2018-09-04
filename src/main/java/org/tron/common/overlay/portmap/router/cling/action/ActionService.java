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

import java.net.URL;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.protocol.sync.SendingAction;
import org.tron.common.overlay.portmap.router.cling.ClingOperationFailedException;
import org.tron.common.overlay.portmap.router.cling.ClingRouterException;

public class ActionService {

  private final RemoteService remoteService;
  private final ControlPoint controlPoint;

  public ActionService(final RemoteService remoteService, final ControlPoint controlPoint) {
    this.remoteService = remoteService;
    this.controlPoint = controlPoint;
  }

  public <T> T run(final ClingAction<T> action) {
    // Figure out the remote URL where we'd like to send the action request to
    final URL controLURL = remoteService.getDevice().normalizeURI(remoteService.getControlURI());

    final ActionInvocation<RemoteService> actionInvocation = action.getActionInvocation();
    final SendingAction prot = controlPoint.getProtocolFactory()
        .createSendingAction(actionInvocation, controLURL);
    prot.run();

    final IncomingActionResponseMessage response = prot.getOutputMessage();
    if (response == null) {
      throw new ClingRouterException("Got null response for action " + actionInvocation);
    } else if (response.getOperation().isFailed()) {
      throw new ClingOperationFailedException(
          "Invocation " + actionInvocation + " failed with operation '"
              + response.getOperation() + "', body '" + response.getBodyString() + "'", response);
    }
    return action.convert(actionInvocation);
  }

  public RemoteService getService() {
    return remoteService;
  }
}
