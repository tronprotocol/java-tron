package org.tron.core.services.jsonrpc.filters;

import static org.tron.common.math.StrictMathWrapper.min;

import com.google.protobuf.ByteString;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.JsonRpcInvalidParamsException;
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

      // if fromBlock is empty but toBlock is not empty,
      // then if toBlock < maxBlockNum, set fromBlock = toBlock
      // then if toBlock >= maxBlockNum, set fromBlock = maxBlockNum
      if (StringUtils.isEmpty(fr.getFromBlock()) && StringUtils.isNotEmpty(fr.getToBlock())) {
        toBlockSrc = JsonRpcApiUtil.getByJsonBlockId(fr.getToBlock(), wallet);
        if (toBlockSrc == -1) {
          toBlockSrc = Long.MAX_VALUE;
        }
        fromBlockSrc = min(toBlockSrc, currentMaxBlockNum);

      } else if (StringUtils.isNotEmpty(fr.getFromBlock())
          && StringUtils.isEmpty(fr.getToBlock())) {
        fromBlockSrc = JsonRpcApiUtil.getByJsonBlockId(fr.getFromBlock(), wallet);
        if (fromBlockSrc == -1) {
          fromBlockSrc = currentMaxBlockNum;
        }
        toBlockSrc = Long.MAX_VALUE;

      } else if (StringUtils.isEmpty(fr.getFromBlock()) && StringUtils.isEmpty(fr.getToBlock())) {
        fromBlockSrc = currentMaxBlockNum;
        toBlockSrc = Long.MAX_VALUE;

      } else {
        fromBlockSrc = JsonRpcApiUtil.getByJsonBlockId(fr.getFromBlock(), wallet);
        toBlockSrc = JsonRpcApiUtil.getByJsonBlockId(fr.getToBlock(), wallet);
        if (fromBlockSrc == -1 && toBlockSrc == -1) {
          fromBlockSrc = currentMaxBlockNum;
          toBlockSrc = Long.MAX_VALUE;
        } else if (fromBlockSrc == -1 && toBlockSrc >= 0) {
          fromBlockSrc = currentMaxBlockNum;
        } else if (fromBlockSrc >= 0 && toBlockSrc == -1) {
          toBlockSrc = Long.MAX_VALUE;
        }
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
