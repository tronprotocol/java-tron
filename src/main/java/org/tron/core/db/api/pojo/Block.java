package org.tron.core.db.api.pojo;

import java.util.List;
import lombok.Data;

@Data(staticConstructor = "of")
public class Block {
  private final String id;
  private final long number;
  private final List<String> transactionIds;
}
