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
package org.tron.application;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

  private static final Logger logger = LoggerFactory.getLogger("Application");
  private Injector injector;
  private ServiceContainer services;

  public Application(Injector injector) {
    this.injector = injector;
    this.services = new ServiceContainer();
  }

  public Injector getInjector() {
    return injector;
  }

  public void addService(Service service) {
    this.services.add(service);
  }

  public void run() {
    this.services.start();
  }

  public void shutdown() {
    logger.info("shutting down");
    this.services.stop();
    System.exit(0);
  }
}
