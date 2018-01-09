package org.tron.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ServiceContainer implements Service {

    private ArrayList<Service> services;

    private static final Logger logger = LoggerFactory.getLogger("Services");

    public ServiceContainer() {
        this.services = new ArrayList<>();
    }

    public void add(Service service) {
        this.services.add(service);
    }

    @Override
    public void start() {
        logger.debug("Starting services");
        for (Service service : this.services) {
            logger.debug("Starting "  + service.getClass().getSimpleName());
            service.start();
        }
    }

    @Override
    public void stop() {
        for (Service service : this.services) {
            logger.debug("Stopping "  + service.getClass().getSimpleName());
            service.stop();
        }
    }
}
