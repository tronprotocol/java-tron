package org.tron.core.services.jsonrpc.filters;

import static org.tron.common.math.StrictMathWrapper.min;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.LATEST_STR;

import com.google.protobuf.ByteString;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.protos.Protocol.Block;

public class LogFilterWrapper {

  @Getter
  private final LogFilter logFilter;

  @Getter
  private final long fromBlock;

  @Getter
  private final long toBlock;

  public LogFilterWrapper(FilterRequest fr, long currentMaxBlockNum, Wallet wallet,
      boolean checkBlockRange) throws JsonRpcInvalidParamsException {

    // 1.convert FilterRequest to LogFilter
    this.logFilter = new LogFilter(fr);

    // 2. get fromBlock、toBlock from FilterRequest
    long fromBlockSrc;
    long toBlockSrc;
    if (fr.getBlockHash() != null) {
      String blockHash = ByteArray.fromHex(fr.getBlockHash());
      if (fr.getFromBlock() != null || fr.getToBlock() != null) {
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
      fromBlockSrc = block.getBlockHeader().getRawData().getNumber();
      toBlockSrc = fromBlockSrc;
    } else {

      // Normalize the request into one of four strategies based on parameter emptiness.
      // Long.MAX_VALUE is an internal sentinel meaning "open upper bound"; it is never
      // treated as a real block number by later query stages.
      // Note: "latest" tag handling differs by strategy:
      // - Strategy 2: toBlock="latest" -> Long.MAX_VALUE (track future blocks)
      // - Strategy 3: fromBlock="latest" -> currentMaxBlockNum snapshot (bounded start)
      // - Strategy 4: fromBlock="latest" -> currentMaxBlockNum; toBlock="latest" -> Long.MAX_VALUE

      boolean fromEmpty = StringUtils.isEmpty(fr.getFromBlock());
      boolean toEmpty = StringUtils.isEmpty(fr.getToBlock());

      if (fromEmpty && toEmpty) {
        // Strategy 1: Both parameters omitted. Start at the current head and track new blocks.
        fromBlockSrc = currentMaxBlockNum;
        toBlockSrc = Long.MAX_VALUE;

      } else if (fromEmpty) {
        // Strategy 2: Only toBlock specified.
        // If toBlock is "latest": track future blocks (fromBlock = currentMaxBlockNum,
        // toBlock = MAX_VALUE). If concrete: bounded query with fromBlock = min(toBlock,
        // currentMaxBlockNum).
        if (LATEST_STR.equalsIgnoreCase(fr.getToBlock())) {
          toBlockSrc = Long.MAX_VALUE;
        } else {
          toBlockSrc = JsonRpcApiUtil.parseBlockNumber(fr.getToBlock(), wallet);
        }
        fromBlockSrc = min(toBlockSrc, currentMaxBlockNum);

      } else if (toEmpty) {
        // Strategy 3: Only fromBlock specified. Start at fromBlock and track new blocks.
        // If fromBlock is "latest", use the snapshot (currentMaxBlockNum) as the starting point.
        fromBlockSrc = LATEST_STR.equalsIgnoreCase(fr.getFromBlock()) ? currentMaxBlockNum
            : JsonRpcApiUtil.parseBlockNumber(fr.getFromBlock(), wallet);
        toBlockSrc = Long.MAX_VALUE;

      } else {
        // Strategy 4: Both parameters specified.
        // If fromBlock is "latest": use the snapshot (currentMaxBlockNum) as a fixed start point.
        // If toBlock is "latest": use Long.MAX_VALUE to track future blocks.
        // Otherwise: parse both as concrete block numbers
        fromBlockSrc = LATEST_STR.equalsIgnoreCase(fr.getFromBlock()) ? currentMaxBlockNum
            : JsonRpcApiUtil.parseBlockNumber(fr.getFromBlock(), wallet);
        toBlockSrc = LATEST_STR.equalsIgnoreCase(fr.getToBlock()) ? Long.MAX_VALUE
            : JsonRpcApiUtil.parseBlockNumber(fr.getToBlock(), wallet);
        if (fromBlockSrc > toBlockSrc) {
          throw new JsonRpcInvalidParamsException("please verify: fromBlock <= toBlock");
        }
      }

      // till now, it needs to check block range for eth_getLogs
      int maxBlockRange = Args.getInstance().getJsonRpcMaxBlockRange();
      if (checkBlockRange && maxBlockRange > 0
          && min(toBlockSrc, currentMaxBlockNum) - fromBlockSrc > maxBlockRange) {
        throw new JsonRpcInvalidParamsException("exceed max block range: " + maxBlockRange);
      }
    }

    this.fromBlock = fromBlockSrc;
    this.toBlock = toBlockSrc;
  }
}
