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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.exception.TronError;

@Slf4j(topic = "app")
@Component
public class ServiceContainer {

  @Autowired
  private List<Service> services;

  private List<Service> enabledServices;

  public ServiceContainer() {
  }

  @PostConstruct
  private void initEnabledServices() {
    this.enabledServices = this.services.stream()
        .filter(Service::isEnable)
        .collect(Collectors.toList());
  }

  void start() {
    logger.info("Starting api services.");
    this.enabledServices.forEach(this::waitForServiceToStart);
    logger.info("All api services started.");
  }

  void stop() {
    logger.info("Stopping api services.");
    this.enabledServices.forEach(this::waitForServiceToStop);
    logger.info("All api services stopped.");
  }

  private void waitForServiceToStart(Service service) {
    final String serviceName = service.getName();
    final CompletableFuture<?> startFuture = service.start();
    do {
      try {
        startFuture.get(60, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new TronError("Interrupted while waiting for service to start", e,
            TronError.ErrCode.API_SERVER_INIT);
      } catch (final ExecutionException e) {
        throw new TronError("Service " + serviceName + " failed to start", e,
            TronError.ErrCode.API_SERVER_INIT);
      } catch (final TimeoutException e) {
        logger.warn("Service {} is taking an unusually long time to start", serviceName);
      }
    } while (!startFuture.isDone());
  }

  private void waitForServiceToStop(Service service) {
    final String serviceName = service.getName();
    final CompletableFuture<?> stopFuture = service.stop();
    try {
      stopFuture.get(30, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      logger.debug("Interrupted while waiting for service {} to complete", serviceName, e);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException e) {
      logger.error("Service {} failed to shutdown", serviceName, e);
    } catch (final TimeoutException e) {
      logger.error("Service {} did not shut down cleanly", serviceName);
    }
  }
}
