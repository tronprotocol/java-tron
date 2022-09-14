package org.tron.program.design.factory;

import org.tron.program.generate.TransactionCreator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liukai
 * @since 2022/9/9.
 */
public class GeneratorFactory {

  public static Map<String, TransactionCreator> creators = new HashMap<>();

  static {


  }


  public static TransactionCreator createFactory(String type) {
    return creators.get(type);
  }

}
