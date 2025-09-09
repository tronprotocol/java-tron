package org.tron.core.utils;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.tron.core.actuator.AbstractActuator;
import org.tron.core.exception.TronError;

@Slf4j(topic = "TransactionRegister")
public class TransactionRegister {

  private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
  private static final String PACKAGE_NAME = "org.tron.core.actuator";

  public static void registerActuator() {
    if (REGISTERED.get()) {
      logger.info("Actuator already registered.");
      return;
    }
    synchronized (TransactionRegister.class) {
      if (REGISTERED.get()) {
        logger.info("Actuator already registered.");
        return;
      }
      logger.info("Register actuator begin.");
      ConfigurationBuilder config = new ConfigurationBuilder()
          .setUrls(ClasspathHelper.forPackage(PACKAGE_NAME))
          .setScanners(new SubTypesScanner());

      Reflections reflections = new Reflections(config);
      Set<Class<? extends AbstractActuator>> subTypes = reflections
          .getSubTypesOf(AbstractActuator.class);
      for (Class<? extends AbstractActuator>  clazz : subTypes) {
        try {
          logger.debug("Registering actuator: {} start", clazz.getName());
          clazz.getDeclaredConstructor().newInstance();
          logger.debug("Registering actuator: {} done", clazz.getName());
        } catch (Exception e) {
          throw new TronError(e, TronError.ErrCode.ACTUATOR_REGISTER);
        }
      }
      REGISTERED.set(true);
      logger.info("Register actuator done, total {}.", subTypes.size());
    }
  }

}
