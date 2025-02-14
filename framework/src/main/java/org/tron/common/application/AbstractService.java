package org.tron.common.application;

import com.google.common.base.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.config.args.Args;


@Slf4j(topic = "service")
public abstract class AbstractService implements Service {

  protected int port;
  @Getter
  protected boolean enable;
  @Getter
  protected final String name = this.getClass().getSimpleName();


  @Override
  public CompletableFuture<Boolean> start() {
    logger.info("{} starting on {}", name, port);
    final CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
    try {
      innerStart();
      resultFuture.complete(true);
      logger.info("{} started, listening on {}", name, port);
    } catch (Exception e) {
      resultFuture.completeExceptionally(e);
    }
    return resultFuture;
  }

  @Override
  public CompletableFuture<Boolean> stop() {
    logger.info("{} shutdown...", name);
    final CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
    try {
      innerStop();
      resultFuture.complete(true);
      logger.info("{} shutdown complete", name);
    } catch (Exception e) {
      resultFuture.completeExceptionally(e);
    }
    return resultFuture;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractService that = (AbstractService) o;
    return port == that.port;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, port);
  }

  public abstract void innerStart() throws Exception;

  public abstract void innerStop() throws Exception;

  protected boolean isFullNode() {
    return !Args.getInstance().isSolidityNode();
  }

}
