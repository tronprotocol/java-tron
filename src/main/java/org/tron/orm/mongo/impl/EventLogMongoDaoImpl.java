package org.tron.orm.mongo.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.tron.orm.mongo.EventLogMongoDao;
import org.tron.orm.mongo.entity.EventLogEntity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

@Repository("eventLogMongoDaoImpl")
public class EventLogMongoDaoImpl<T> implements EventLogMongoDao<T> {

  private static Logger log = LoggerFactory.getLogger(EventLogMongoDaoImpl.class);

  protected Class<T> entityClass;

  @Autowired
  private MongoTemplate mongoTemplate;

  public EventLogMongoDaoImpl() {
    Type superType = getClass().getGenericSuperclass();
    if (superType instanceof Class) {
      entityClass = (Class<T>) superType;
    }
    if (superType instanceof ParameterizedType) {
      entityClass = (Class<T>) ((ParameterizedType) superType).getActualTypeArguments()[0];
    }

  }

  @Override
  public void insert(Object object, String collectionName) {
    mongoTemplate.insert(object, collectionName);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddressHexString, String collectionName) {

    Query query = new Query(Criteria.where("contractAddressHexString").is(contractAddressHexString));
    return mongoTemplate.find(query, EventLogEntity.class, collectionName);

  }

  @Override
  public EventLogEntity findOne(String contractAddressHexString, String collectionName) {

    Query query = new Query(Criteria.where("contractAddressHexString").is(contractAddressHexString));
    return mongoTemplate.findOne(query, EventLogEntity.class, collectionName);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName, String collectionName) {
    Query query = new Query(Criteria.where("contractAddressHexString").and(contractAddressHexString).where("entryName").is(entryName));
    return mongoTemplate.find(query, EventLogEntity.class, collectionName);
  }

  @Override
  public EventLogEntity findOne(String contractAddressHexString, String entryName, String collectionName) {
    Query query = new Query(Criteria.where("contractAddressHexString").and(contractAddressHexString).is(contractAddressHexString).where("entryName").is(entryName));
    return mongoTemplate.findOne(query, EventLogEntity.class, collectionName);
  }

  @Override
  public List<EventLogEntity> findAll(String contractAddressHexString, String entryName, long blockNumber, String collectionName) {
    Query query = new Query(Criteria.where("contractAddressHexString").and(contractAddressHexString).where("entryName").and(entryName).is(entryName).where("blockNumber").is(blockNumber));
    return mongoTemplate.find(query, EventLogEntity.class, collectionName);
  }

  @Override
  public EventLogEntity findOne(String contractAddressHexString, String entryName, long blockNumber, String collectionName) {
    Query query = new Query(Criteria.where("contractAddressHexString").and(contractAddressHexString).and(contractAddressHexString).where("entryName").is(entryName).where("blockNumber").is(blockNumber));
    return mongoTemplate.findOne(query, EventLogEntity.class, collectionName);
  }

}
