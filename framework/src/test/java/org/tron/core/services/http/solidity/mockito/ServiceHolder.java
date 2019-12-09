package org.tron.core.services.http.solidity.mockito;

import java.util.HashSet;
import java.util.Set;

/**
 * @author alberto
 * @version 1.0.0
 * @Description
 * @date 2019-12-09 00:26
 **/
public class ServiceHolder {

  private final Set<Object> services = new HashSet<Object>();

  public void addService(Object service) {
    services.add(service);
  }

  public void removeService(Object service) {
    services.remove(service);
  }
}