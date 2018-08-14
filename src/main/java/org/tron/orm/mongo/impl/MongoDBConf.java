package org.tron.orm.mongo.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.ArrayList;
import java.util.List;

@Configuration
@PropertySource(value = "classpath:mongodb.properties")
@EnableMongoRepositories(basePackages = "org.tron.orm.mongo.impl")
public class MongoDBConf {
  @Autowired
  private Environment environment;

  @Bean
  public MongoClient mongoClient() {
    String username = environment.getProperty("mongo.username");
    String password = environment.getProperty("mongo.password");
    String database = environment.getProperty("mongo.dbname");
    String host = environment.getProperty("mongo.host");
    Integer port = Integer.valueOf(environment.getProperty("mongo.port"));
    ServerAddress serverAddress = new ServerAddress(host, port);
    MongoCredential mongoCredential = MongoCredential.createCredential(username, database, password.toCharArray());
    List<MongoCredential> mongoCredentialList = new ArrayList<>();
    mongoCredentialList.add(mongoCredential);

    return new MongoClient(serverAddress, mongoCredentialList);
  }

  @Bean
  public MongoDbFactory mongoDbFactory() {
    String database = environment.getProperty("mongo.dbname");
    return new SimpleMongoDbFactory(mongoClient(), database);
  }

  private MongoTemplate mongoTemplate;

  @Bean
  public MongoTemplate mongoTemplate() {

    MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory());
    return mongoTemplate;
  }
}


