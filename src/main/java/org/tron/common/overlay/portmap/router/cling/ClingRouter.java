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

import java.net.URI;
import java.util.Collection;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.meta.UDAVersion;
import org.fourthline.cling.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.portmap.model.PortMapping;
import org.tron.common.overlay.portmap.model.Protocol;
import org.tron.common.overlay.portmap.router.AbstractRouter;
import org.tron.common.overlay.portmap.router.RouterException;
import org.tron.common.overlay.portmap.router.cling.action.ActionService;
import org.tron.common.overlay.portmap.router.cling.action.AddPortMappingAction;
import org.tron.common.overlay.portmap.router.cling.action.DeletePortMappingAction;
import org.tron.common.overlay.portmap.router.cling.action.GetExternalIpAction;

public class ClingRouter extends AbstractRouter {

  /**
   * The maximum number of port mappings that we will try to retrieve from the router.
   */
  private final static int MAX_NUM_PORTMAPPINGS = 500;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final RemoteService service;

  private final Registry registry;

  private final ActionService actionService;

  public ClingRouter(final RemoteService service, final Registry registry,
      final ControlPoint controlPoint) {
    super(getName(service));
    this.service = service;
    this.registry = registry;
    actionService = new ActionService(service, controlPoint);
  }

  private static String getName(final Service<?, ?> service) {
    return service.getDevice().getDisplayString();
  }

  @Override
  public String getExternalIPAddress() throws RouterException {
    return actionService.run(new GetExternalIpAction(service));
  }

  @Override
  public String getInternalHostName() {
    final URI uri = getUri();
    return uri != null ? uri.getHost() : null;
  }

  @Override
  public int getInternalPort() throws RouterException {
    final URI uri = getUri();
    return uri != null ? uri.getPort() : null;
  }

  private URI getUri() {
    if (service.getDevice().getDetails().getPresentationURI() != null) {
      return service.getDevice().getDetails().getPresentationURI();
    }
    if (service.getControlURI() != null) {
      return service.getControlURI();
    }
    if (service.getDescriptorURI() != null) {
      return service.getDescriptorURI();
    }
    if (service.getEventSubscriptionURI() != null) {
      return service.getEventSubscriptionURI();
    }
    return null;
  }

  @Override
  public Collection<PortMapping> getPortMappings() throws RouterException {
    return new ClingPortMappingExtractor(actionService, MAX_NUM_PORTMAPPINGS).getPortMappings();
  }

  @Override
  public void logRouterInfo() throws RouterException {
    final RemoteDevice device = service.getDevice();
    final UDAVersion version = device.getVersion();
    final DeviceDetails deviceDetails = device.getDetails();
    logger.info("Service id: {}", service.getServiceId());
    logger.info("Reference: {}", service.getReference());
    logger.info("Display name: {}", device.getDisplayString());
    logger.info("Version: {}.{}", version.getMajor(), version.getMinor());
    logger.info("Control uri: {}", service.getControlURI());
    logger.info("Descriptor uri: {}", service.getDescriptorURI());
    logger.info("Event subscription uri: {}", service.getEventSubscriptionURI());
    logger.info("Device base url: {}", deviceDetails.getBaseURL());
    logger.info("Device presentation uri: {}", deviceDetails.getPresentationURI());
  }

  @Override
  public void addPortMappings(final Collection<PortMapping> mappings) throws RouterException {
    for (final PortMapping portMapping : mappings) {
      addPortMapping(portMapping);
    }
  }

  @Override
  public void addPortMapping(final PortMapping mapping) throws RouterException {
    actionService.run(new AddPortMappingAction(service, mapping));
  }

  @Override
  public void removeMapping(final PortMapping mapping) throws RouterException {
    actionService.run(new DeletePortMappingAction(service, mapping));
  }

  @Override
  public void removePortMapping(final Protocol protocol, final String remoteHost,
      final int externalPort)
      throws RouterException {
    removeMapping(new PortMapping(protocol, remoteHost, externalPort, null, 0, null));
  }

  @Override
  public void disconnect() {
    logger.debug("Shutdown registry");
    registry.shutdown();
  }
}
