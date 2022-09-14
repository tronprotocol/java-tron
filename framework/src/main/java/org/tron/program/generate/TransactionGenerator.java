package org.tron.program.generate;

import org.tron.program.design.factory.GeneratorFactory;

import java.util.List;

/**
 * @author liukai
 * @since 2022/9/9.
 */
public class TransactionGenerator {

  private int count;
  private String type;

  public TransactionGenerator(int count, String type) {
    this.count = count;
    this.type = type;
  }

  public List<String> create() {
    //types
    TransactionCreator generator = GeneratorFactory.getGenerator(type);
    return generator.createTransactions(count);
  }

}
