package org.tron.orm.service;

import org.tron.orm.mongo.entity.EventLogEntity;

import java.util.List;

/**
 * 事件日志service类基础接口
 */
public interface EventLogService {

  public void insertEventLog(String eventLog);

  public List<EventLogEntity> findAll(String contractAddressHexString);

  public EventLogEntity findOne(String contractAddressHexString);

  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName);

  public EventLogEntity findOne(String contractAddressHexString, String entryName);

  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName, long blockNumber);

  public EventLogEntity findOne(String contractAddressHexString, String entryName, long blockNumber);
}
