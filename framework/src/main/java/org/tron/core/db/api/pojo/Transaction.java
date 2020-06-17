package org.tron.core.db.api.pojo;

import lombok.Data;

@Data(staticConstructor = "of")
public class Transaction {

  private String id;
  private String from;
  private String to;
}
