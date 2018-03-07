package org.tron.core.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissDoTuple {
  private TronDatabase database;
  private byte[] key;
}
