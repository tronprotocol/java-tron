/**
 * UPnP PortMapper - A tool for managing port forwardings via UPnP
 * Copyright (C) 2015 Christoph Pirkl <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.portmap.router.cling;

import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;

public class ClingOperationFailedException extends ClingRouterException {

    private static final long serialVersionUID = 1L;
    private final IncomingActionResponseMessage response;

    public ClingOperationFailedException(final String message, final IncomingActionResponseMessage response) {
        super(message);
        assert response.getOperation().isFailed();
        this.response = response;
    }

    public IncomingActionResponseMessage getResponse() {
        return response;
    }
}
