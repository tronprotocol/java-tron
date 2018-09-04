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
package org.tron.common.overlay.portmap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.portmap.model.PortMapping;
import org.tron.common.overlay.portmap.model.Protocol;
import org.tron.common.overlay.portmap.router.AbstractRouterFactory;
import org.tron.common.overlay.portmap.router.IRouter;
import org.tron.common.overlay.portmap.router.RouterException;
import org.tron.common.overlay.portmap.router.cling.ClingRouterFactory;
import org.tron.common.overlay.portmap.router.dummy.DummyRouterFactory;
import org.tron.common.overlay.portmap.router.weupnp.WeUPnPRouterFactory;

//Example: java -jar PortMapper.jar -add -externalPort 22 -internalPort 22 [-ip <ip-addr>] -description desc
public class PortMapperService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private String routerFactoryClassName = ClingRouterFactory.class.getName();
  private AbstractRouterFactory routerFactory;
  private IRouter router;

  public PortMapperService() {
  }

  public PortMapperService(String routerFactoryClassName) {
    this.routerFactoryClassName = routerFactoryClassName;
  }

  public boolean start() {
    try {
      router = connect();
      return router != null;
    } catch (Exception e) {
      logger.error("An error occured", e);
    }
    return false;
  }

  public void destroy() {
    try {
      if (router != null) {
        router.close();
      }
      if (routerFactory != null) {
        routerFactory.shutdownService();
      }
    } catch (Exception e) {
      logger.error("port mapper destroy error!", e);
    }
  }

  private void printPortForwardings() throws RouterException {
    Collection<PortMapping> mappings = router.getPortMappings();
    if (CollectionUtils.isEmpty(mappings)) {
      logger.info("No port mappings found");
      return;
    }
    StringBuilder b = new StringBuilder();
    for (Iterator<PortMapping> iterator = mappings.iterator(); iterator.hasNext(); ) {
      PortMapping mapping = iterator.next();
      b.append(mapping.getCompleteDescription());
      if (iterator.hasNext()) {
        b.append("\n");
      }
    }
    logger.info("Found " + mappings.size() + " port forwardings:\n" + b.toString());
  }

  public void deletePortForwardings(Protocol protocol, int externalPort) throws RouterException {
    if (router == null) {
      return;
    }
    logger.info("Deleting mapping for protocol " + protocol + " and external port " + externalPort);
    router.removePortMapping(protocol, null, externalPort);
    printPortForwardings();
  }

  private void printStatus() throws RouterException {
    router.logRouterInfo();
  }

  public void addPortForwarding(Protocol protocol, int internalPort, int externalPort,
      String internalIp, String desc) throws RouterException {

    String internalClient = StringUtils.isNotBlank(internalIp) ? internalIp
        : router.getLocalHostAddress();
    String description = StringUtils.isNotBlank(desc) ? desc
        : "PortMapper " + protocol + "/" + internalClient + ":" + internalPort;
    PortMapping mapping = new PortMapping(protocol, null, externalPort, internalClient,
        internalPort, description);
    logger.info("Adding mapping " + mapping);
    router.addPortMapping(mapping);
    printPortForwardings();
  }

  private AbstractRouterFactory createRouterFactory() throws RouterException {
    logger.info("Creating router factory for class {}", routerFactoryClassName);
    if (ClingRouterFactory.class.getName().equals(routerFactoryClassName)) {
      return new ClingRouterFactory();
    } else if (DummyRouterFactory.class.getName().equals(routerFactoryClassName)) {
      return new DummyRouterFactory();
    } else if (WeUPnPRouterFactory.class.getName().equals(routerFactoryClassName)) {
      return new WeUPnPRouterFactory();
    } else {
      throw new RouterException(
          "Error creating a router factory using class " + routerFactoryClassName);
    }
  }

  private IRouter connect() throws RouterException {
    routerFactory = createRouterFactory();
    logger.info("Searching for routers...");
    return selectRouter(routerFactory.findRouters());
  }

  private IRouter selectRouter(final List<IRouter> foundRouters) {
    // One router found: use it.
    if (CollectionUtils.isEmpty(foundRouters)) {
      return null;
    } else {
      logger.info("Connected to router " + foundRouters.get(0).getName());
      return foundRouters.get(0);
    }
  }
}
