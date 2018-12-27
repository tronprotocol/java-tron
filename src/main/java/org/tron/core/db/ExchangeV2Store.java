package org.tron.core.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExchangeV2Store extends ExchangeStore {

  @Autowired
  public ExchangeV2Store(@Value("exchange-v2") String dbName) {
    super(dbName);
  }

}