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
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.portmap.model.Protocol;
import org.tron.core.config.args.Args;

@Slf4j
@Component
public class PortMapperStarter {

  private Protocol tcp = Protocol.TCP;
  private Protocol udp = Protocol.UDP;
  private int port = Args.getInstance().getNodeListenPort();

  private ExecutorService executorService;
  private PortMapperService portMapperService;

  @PostConstruct
  public void init() {
    if (Args.getInstance().isOpenPortMapper()) {
      portMapperService = new PortMapperService();
      executorService = Executors.newFixedThreadPool(1, r -> new Thread(r, "port-mapper-service"));
      executorService.submit(() -> discoverPortMappers());
    }
  }

  @PreDestroy
  public void destroy() {
    try {
      portMapperService.deletePortForwardings(tcp, port);
      portMapperService.deletePortForwardings(udp, port);
//      portMapperService.destroy();
      executorService.shutdown();
    } catch (Exception e) {
      logger.error("destroy the port mapper error!", e);
    }
    logger.info("destroy the port mapper success");
  }

  public void discoverPortMappers() {
    try {
      if (portMapperService.start()) {
        portMapperService.addPortForwarding(tcp, port, port, null, null);
        portMapperService.addPortForwarding(udp, port, port, null, null);
        logger.info("port mapper success,port is {}", port);
      } else {
        logger.info("can not find the mapper device");
        destroy();
      }
    } catch (Exception e) {
      logger.error("port mapper fail!", e);
    }
  }
}
