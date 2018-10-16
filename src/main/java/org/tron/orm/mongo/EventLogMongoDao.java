package org.tron.orm.mongo;

import org.tron.orm.mongo.entity.EventLogEntity;

import java.util.List;

/**
 * 事件日志MongoDB基础接口
 *
 * @param <T>
 */
public interface EventLogMongoDao<T> extends MongoBaseDao {
  public List<EventLogEntity> findAll(String contractAddress, String collectionName);

  public EventLogEntity findOne(String contractAddress, String collectionName);

  public List<EventLogEntity> findAll(String contractAddress, String entryName, String collectionName);

  public EventLogEntity findOne(String contractAddress, String entryName, String collectionName);

  public List<EventLogEntity> findAll(String contractAddress, String entryName, long blockNumber, String collectionName);

  public EventLogEntity findOne(String contractAddress, String entryName, long blockNumber, String collectionName);

  public List<EventLogEntity> findAllByTransactionId(String transactionId, String collectionName);

  public EventLogEntity findOneByTransactionId(String transactionId, String collectionName);
}
