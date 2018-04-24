package org.tron.core.db.api.pojo;

import lombok.Data;

@Data(staticConstructor = "of")
public class Witness {

  private String address;
  private String publicKey;
  private String url;
  private boolean jobs;
}
