package org.tron.orm.mongo;

/**
 * MongoDB 基础接口
 *
 * @param <T>
 */
public interface MongoBaseDao<T> {

  public void insert(T object, String collectionName);


}
