package org.tron.orm.mongo;

/**
 * MongoDB
 *
 * @param <T>
 */
public interface MongoBaseDao<T> {

  public void insert(T object, String collectionName);

}
