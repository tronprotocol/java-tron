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
  public void insertEventLog(String eventLog) {
    eventLogMongoDao.insert(eventLog, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddressHexString) {
    return eventLogMongoDao.findAll(contractAddressHexString, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public EventLogEntity findOne(String contractAddressHexString) {
    return eventLogMongoDao.findOne(contractAddressHexString, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName) {
    return eventLogMongoDao.findAll(contractAddressHexString, entryName, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public EventLogEntity findOne(String contractAddressHexString, String entryName) {
    return eventLogMongoDao.findOne(contractAddressHexString, entryName, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName, long blockNumber) {
    return eventLogMongoDao.findAll(contractAddressHexString, entryName, blockNumber, COLLECTION_EVENT_LOG_CENTER);
  }

  @Override
  public EventLogEntity findOne(String contractAddressHexString, String entryName, long blockNumber) {
    return eventLogMongoDao.findOne(contractAddressHexString, entryName, blockNumber, COLLECTION_EVENT_LOG_CENTER);
  }


}
