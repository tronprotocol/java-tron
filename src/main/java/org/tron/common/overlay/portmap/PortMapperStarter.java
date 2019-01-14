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

import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.portmap.model.Protocol;
import org.tron.core.config.args.Args;

@Slf4j
public class PortMapperStarter {

  public static final int PORT_LIMIT = 65535;
  private Protocol tcp = Protocol.TCP;
  private Protocol udp = Protocol.UDP;
  private int port = Args.getInstance().getNodeListenPort();
  private int externalPort = Args.getInstance().getExternalPort();
  private volatile boolean mapper = false;

  private ExecutorService executorService;
  private PortMapperService portMapperService;

  public void init() {
    if (Args.getInstance().isOpenPortMapper()) {
      portMapperService = new PortMapperService();
//      executorService = Executors.newFixedThreadPool(1, r -> new Thread(r, "port-mapper-service"));
//      executorService.submit(() -> discoverPortMappers());
      discoverPortMappers();
    }
  }

  public void destroy() {
    try {
      if (Args.getInstance().isOpenPortMapper() && mapper) {
        portMapperService.deletePortForwardings(tcp, externalPort);
        portMapperService.deletePortForwardings(udp, externalPort);
//        portMapperService.destroy();
        executorService.shutdown();
      }
    } catch (Exception e) {
      logger.error("destroy the port mapper error!", e);
    }
    logger.info("destroy the port mapper success");
  }

  public void discoverPortMappers() {
    while (port < PORT_LIMIT) {
      try {
        if (portMapperService.start()) {
          portMapperService.addPortForwarding(tcp, port, externalPort, null, null);
          portMapperService.addPortForwarding(udp, port, externalPort, null, null);
          mapper = true;
          //modify the home node port
          Args.getInstance().setExternalPort(externalPort);
          logger.info("port mapper success,internalPort is {},externalPort is {}", port,
              externalPort);
        } else {
          logger.info("can not find the mapper device");
          destroy();
        }
      } catch (Exception e) {
        ++externalPort;
        logger.error("port mapper fail!", e);
      }
    }
  }
}
