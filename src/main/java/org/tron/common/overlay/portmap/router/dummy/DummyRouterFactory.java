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
package org.tron.common.overlay.portmap.router.dummy;

import java.util.LinkedList;
import java.util.List;
import org.tron.common.overlay.portmap.router.AbstractRouterFactory;
import org.tron.common.overlay.portmap.router.IRouter;
import org.tron.common.overlay.portmap.router.RouterException;

/**
 * Router factory for testing without a real router.
 */
public class DummyRouterFactory extends AbstractRouterFactory {

  public DummyRouterFactory() {
    super("Dummy library");
  }

  @Override
  protected List<IRouter> findRoutersInternal() throws RouterException {
    final List<IRouter> routers = new LinkedList<>();
    routers.add(new DummyRouter("DummyRouter1"));
    routers.add(new DummyRouter("DummyRouter2"));
    return routers;
  }

  @Override
  protected IRouter connect(final String locationUrl) throws RouterException {
    return new DummyRouter("DummyRouter @ " + locationUrl);
  }
}
