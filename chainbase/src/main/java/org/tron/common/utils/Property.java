package org.tron.common.utils;

import lombok.Getter;
import lombok.Setter;
import org.iq80.leveldb.Options;

public class Property {

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String path;

  @Getter
  @Setter
  private Options dbOptions;
}
