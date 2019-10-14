package org.tron.core.db.api.pojo;

import java.util.List;
import lombok.Data;

@Data(staticConstructor = "of")
public class Block {

  private String id;
  private long number;
  private List<String> transactionIds;
}
