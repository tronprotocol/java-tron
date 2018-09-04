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
package org.tron.common.overlay.portmap.router.cling;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.meta.RemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.portmap.router.AbstractRouterFactory;
import org.tron.common.overlay.portmap.router.IRouter;
import org.tron.common.overlay.portmap.router.RouterException;

public class ClingRouterFactory extends AbstractRouterFactory {

  private static final long DISCOVERY_TIMEOUT_SECONDS = 5;
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private UpnpService upnpService;

  public ClingRouterFactory() {
    super("Cling lib");
  }

  @Override
  protected List<IRouter> findRoutersInternal() throws RouterException {
    final UpnpServiceConfiguration config = new DefaultUpnpServiceConfiguration();
    final ClingRegistryListener clingRegistryListener = new ClingRegistryListener();
    upnpService = new UpnpServiceImpl(config, clingRegistryListener);

    log.debug("Start searching using upnp service");
    upnpService.getControlPoint().search();
    final RemoteService service = (RemoteService) clingRegistryListener
        .waitForServiceFound(DISCOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    if (service == null) {
      log.debug("Did not find a service after {} seconds", DISCOVERY_TIMEOUT_SECONDS);
      return Collections.emptyList();
    }

    log.debug("Found service {}", service);
    return Arrays.<IRouter>asList(
        new ClingRouter(service, upnpService.getRegistry(), upnpService.getControlPoint()));
  }

  public void shutdownService() {
    log.debug("Shutdown upnp service");
    upnpService.shutdown();
  }

  @Override
  protected IRouter connect(final String locationUrl) throws RouterException {
    throw new UnsupportedOperationException(
        "Direct connection via location URL is not supported for Cling library.");
  }
}
