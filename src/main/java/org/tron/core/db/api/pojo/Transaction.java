package org.tron.core.db.api.pojo;

import lombok.Data;

@Data(staticConstructor = "of")
public class Transaction {

  private final String id;
  private final String from;

  private final String to;
}
