/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.services.interfaceOnSolidity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

@Slf4j(topic = "API")
@Component
public class WalletOnSolidity {

  enum ApiType {
    HTTP,
    GRPC
  }

  private ListeningExecutorService httpExecutorService = MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(Args.getInstance().getSolidityThreads()/2,
          new ThreadFactoryBuilder().setNameFormat("WalletOnSolidity-HTTP-%d").build()));
  private ListeningExecutorService rpcExecutorService = MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(Args.getInstance().getSolidityThreads()/2,
          new ThreadFactoryBuilder().setNameFormat("WalletOnSolidity-GRPC-%d").build()));

  @Autowired
  private Manager dbManager;

  private  <T> T futureGet(ListeningExecutorService service, ApiType type, Callable<T> callable) {
    ListenableFuture<T> future = service.submit(() -> {
      try {
        dbManager.setMode(false);
        return callable.call();
      } catch (Exception e) {
        logger.info(type + " futureGet " + e.getMessage());
        return null;
      }
    });

    try {
      return future.get(20000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException ignored) {
    } catch (TimeoutException e) {
      logger.info(type + " futureGet time out");
    }

    return null;
  }

  private void futureGet(ListeningExecutorService service, ApiType type, Runnable runnable) {
    ListenableFuture<?> future = service.submit(() -> {
      try {
        dbManager.setMode(false);
        runnable.run();
      } catch (Exception e) {
        logger.info(type + " futureGet " + e.getMessage());
      }
    });

    try {
      future.get(20000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException ignored) {
    } catch (TimeoutException e) {
      logger.info(type + " futureGet time out");
    }
  }

  public <T> T futureGet(Callable<T> callable) {
    return futureGet(httpExecutorService, ApiType.HTTP, callable);
  }

  public void futureGet(Runnable runnable) {
    futureGet(httpExecutorService, ApiType.HTTP, runnable);
  }

  public <T> T rpcFutureGet(Callable<T> callable) {
    return futureGet(rpcExecutorService, ApiType.GRPC, callable);
  }

  public void rpcFutureGet(Runnable runnable) {
    futureGet(rpcExecutorService, ApiType.GRPC, runnable);
  }
}
