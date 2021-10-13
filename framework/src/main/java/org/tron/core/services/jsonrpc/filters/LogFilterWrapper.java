package org.tron.core.services.jsonrpc.filters;

import com.google.protobuf.ByteString;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.protos.Protocol.Block;

public class LogFilterWrapper {

  @Getter
  private LogFilter logFilter;

  @Getter
  private long fromBlock;

  @Getter
  private long toBlock;

  public LogFilterWrapper(FilterRequest fr, long currentMaxBlockNum, Wallet wallet)
      throws JsonRpcInvalidParamsException {

    // 1.convert FilterRequest to LogFilter
    this.logFilter = new LogFilter(fr);

    // 2. get fromBlock、toBlock from FilterRequest
    long fromBlock;
    long toBlock;
    if (fr.blockHash != null) {
      String blockHash = ByteArray.fromHex(fr.blockHash);
      if (fr.fromBlock != null || fr.toBlock != null) {
        throw new JsonRpcInvalidParamsException(
            "cannot specify both BlockHash and FromBlock/ToBlock, choose one or the other");
      }
      Block block = null;
      if (wallet != null) {
        block = wallet.getBlockById(ByteString.copyFrom(ByteArray.fromHexString(blockHash)));
      }
      if (block == null) {
        throw new JsonRpcInvalidParamsException("invalid blockHash");
      }
      fromBlock = block.getBlockHeader().getRawData().getNumber();
      toBlock = fromBlock;
    } else {

      // if fromBlock is empty but toBlock is not empty,
      // then if toBlock < maxBlockNum, set fromBlock = toBlock
      // then if toBlock >= maxBlockNum, set fromBlock = maxBlockNum
      if (StringUtils.isEmpty(fr.fromBlock) && StringUtils.isNotEmpty(fr.toBlock)) {
        toBlock = JsonRpcApiUtil.getByJsonBlockId(fr.toBlock);
        if (toBlock == -1) {
          toBlock = Long.MAX_VALUE;
        }
        if (toBlock < currentMaxBlockNum) {
          fromBlock = toBlock;
        } else {
          fromBlock = currentMaxBlockNum;
        }

      } else if (StringUtils.isNotEmpty(fr.fromBlock) && StringUtils.isEmpty(fr.toBlock)) {
        fromBlock = JsonRpcApiUtil.getByJsonBlockId(fr.fromBlock);
        if (fromBlock == -1) {
          fromBlock = currentMaxBlockNum;
        }
        toBlock = Long.MAX_VALUE;

      } else if (StringUtils.isEmpty(fr.fromBlock) && StringUtils.isEmpty(fr.toBlock)) {
        fromBlock = currentMaxBlockNum;
        toBlock = Long.MAX_VALUE;

      } else {
        fromBlock = JsonRpcApiUtil.getByJsonBlockId(fr.fromBlock);
        toBlock = JsonRpcApiUtil.getByJsonBlockId(fr.toBlock);
        if (fromBlock == -1 && toBlock == -1) {
          fromBlock = currentMaxBlockNum;
          toBlock = Long.MAX_VALUE;
        } else if (fromBlock == -1 && toBlock >= 0) {
          fromBlock = currentMaxBlockNum;
        } else if (fromBlock >= 0 && toBlock == -1) {
          toBlock = Long.MAX_VALUE;
        }
        if (fromBlock > toBlock) {
          throw new JsonRpcInvalidParamsException("please verify: fromBlock <= toBlock");
        }
      }
    }

    this.fromBlock = fromBlock;
    this.toBlock = toBlock;
  }
}
