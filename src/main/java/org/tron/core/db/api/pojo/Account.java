package org.tron.core.db.api.pojo;

import lombok.Data;

@Data(staticConstructor = "of")
public class Account {
  private final String address;
  private final String name;
  private final long balance;
}
