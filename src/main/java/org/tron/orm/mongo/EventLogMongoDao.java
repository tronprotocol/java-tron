package org.tron.orm.mongo;

import org.tron.orm.mongo.entity.EventLogEntity;

import java.util.List;

/**
 * 事件日志MongoDB基础接口
 *
 * @param <T>
 */
public interface EventLogMongoDao<T> extends MongoBaseDao {

  public List<EventLogEntity> findAll(String contractAddressHexString, String collectionName);

  public EventLogEntity findOne(String contractAddressHexString, String collectionName);

  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName, String collectionName);

  public EventLogEntity findOne(String contractAddressHexString, String entryName, String collectionName);

  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName, long blockNumber, String collectionName);

  public EventLogEntity findOne(String contractAddressHexString, String entryName, long blockNumber, String collectionName);


}
