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
import com.google.protobuf.ByteString;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.core.Wallet;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;

@Slf4j
@Component
public class WalletOnSolidity {

  ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(5,
          new ThreadFactoryBuilder().setNameFormat("WalletOnSolidity-%d").build()));

  @Autowired
  private Manager dbManager;
  @Autowired
  private Wallet wallet;

  public <T> T futureGet(Callable<T> callable) {
    ListenableFuture<T> future = executorService.submit(() -> {
      dbManager.getRevokingStore().setMode(false);
      return callable.call();
    });

    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException ignored) {

    }

    return null;
  }

  public void futureGet(Runnable runnable) {
    ListenableFuture<?> future = executorService.submit(() -> {
      dbManager.getRevokingStore().setMode(false);
      runnable.run();
    });

    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException ignored) {

    }
  }

  public Account getAccount(Account account) {
    return futureGet(() -> wallet.getAccount(account));
  }


  public Account getAccountById(Account account) {
    return futureGet(() -> wallet.getAccountById(account));
  }

  public Block getNowBlock() {
    return futureGet(() -> wallet.getNowBlock());
  }

  public Block getBlockByNum(long blockNum) {
    return futureGet(() -> wallet.getBlockByNum(blockNum));
  }

  public WitnessList getWitnessList() {
    return futureGet(() -> wallet.getWitnessList());
  }

  public AssetIssueList getAssetIssueList() {
    return futureGet(() -> wallet.getAssetIssueList());
  }


  public AssetIssueList getAssetIssueList(long offset, long limit) {
    return futureGet(() -> wallet.getAssetIssueList(offset, limit));
  }

  public Transaction getTransactionById(ByteString transactionId) {
    return futureGet(() -> wallet.getTransactionById(transactionId));
  }

  public TransactionInfo getTransactionInfoById(ByteString transactionId) {
    return futureGet(() -> wallet.getTransactionInfoById(transactionId));
  }

  public DelegatedResourceList getDelegatedResource(ByteString fromAddress, ByteString toAddress) {
    return futureGet(() -> wallet.getDelegatedResource(fromAddress, toAddress));
  }

  public DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(ByteString address) {
    return futureGet(() -> wallet.getDelegatedResourceAccountIndex(address));
  }
}
