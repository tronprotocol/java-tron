package org.tron.orm.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.orm.mongo.entity.EventLogEntity;
import org.tron.orm.mongo.impl.EventLogMongoDaoImpl;
import org.tron.orm.service.EventLogService;

import java.util.List;

/**
 * 事件日志service实现类
 */
@Service
public class EventLogServiceImpl implements EventLogService {

  @Autowired
  private EventLogMongoDaoImpl eventLogMongoDao;

  private static final String COLLECTION_EVENT_LOG_CENTER = "eventLog";

  @Override
  public void insertEventLog(EventLogEntity eventLogEntity) {
    eventLogMongoDao.insert(eventLogEntity, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddress) {
    return eventLogMongoDao.findAll(contractAddress, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public EventLogEntity findOne(String contractAddress) {
    return eventLogMongoDao.findOne(contractAddress, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddress, String eventName) {
    return eventLogMongoDao.findAll(contractAddress, eventName, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public EventLogEntity findOne(String contractAddress, String eventName) {
    return eventLogMongoDao.findOne(contractAddress, eventName, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddress, String eventName, long blockNumber) {
    return eventLogMongoDao.findAll(contractAddress, eventName, blockNumber, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public EventLogEntity findOne(String contractAddress, String eventName, long blockNumber) {
    return eventLogMongoDao.findOne(contractAddress, eventName, blockNumber, COLLECTION_EVENT_LOG_CENTER);
  }


}
