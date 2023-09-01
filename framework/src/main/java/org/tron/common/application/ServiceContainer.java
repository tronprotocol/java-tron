/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.application;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;

@Slf4j(topic = "app")
public class ServiceContainer {

  private final Set<Service> services;

  public ServiceContainer() {
    this.services = Collections.synchronizedSet(new LinkedHashSet<>());
  }

  public void add(Service service) {
    this.services.add(service);
  }


  public void init() {
    this.services.forEach(service -> {
      logger.debug("Initing {}.", service.getClass().getSimpleName());
      service.init();
    });
  }

  public void init(CommonParameter parameter) {
    this.services.forEach(service -> {
      logger.debug("Initing {}.", service.getClass().getSimpleName());
      service.init(parameter);
    });
  }

  public void start() {
    logger.info("Starting api services.");
    this.services.forEach(service -> {
      logger.debug("Starting {}.", service.getClass().getSimpleName());
      service.start();
    });
    logger.info("All api services started.");
  }

  public void stop() {
    logger.info("Stopping api services.");
    this.services.forEach(service -> {
      logger.debug("Stopping {}.", service.getClass().getSimpleName());
      service.stop();
    });
    logger.info("All api services stopped.");
  }

  public void blockUntilShutdown() {
    this.services.stream().findFirst().ifPresent(Service::blockUntilShutdown);
  }
}
