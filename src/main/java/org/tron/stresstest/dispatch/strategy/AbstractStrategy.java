package org.tron.stresstest.dispatch.strategy;

import java.util.List;
import java.util.Random;
import javax.annotation.PostConstruct;
import lombok.Setter;
import lombok.ToString;


@ToString(exclude = "random")
public abstract class AbstractStrategy<T extends Bucket> extends Bucket implements IStrategy<T> {

  private Random random = new Random(System.currentTimeMillis());

  @Setter
  protected List<T> source;
  @Setter
  protected String name;

  @PostConstruct
  public void check() {
    int bucket = source == null ? 0 : source.stream()
        .mapToInt(b -> b.end - b.begin + 1)
        .sum();
    if (bucket != 0 && bucket != 100) {
      throw new IllegalArgumentException("bucket sum not equals 100.");
    }
  }

  @Override
  public T dispatch() {
    int randomInt = random.nextInt(100);

    return source.stream()
        .filter(t -> t.begin <= randomInt && t.end >= randomInt)
        .findFirst()
        .orElse(null);
  }

}
