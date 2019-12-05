package org.tron.core.db2;

public interface ISession extends AutoCloseable {

  void commit();

  void revoke();

  void merge();

  void destroy();

  void close();

}
