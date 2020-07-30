package org.tron.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.Valid;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.Wallet;
import org.tron.model.BlockIdentifier;
import org.tron.model.BlockRequest;
import org.tron.model.BlockResponse;
import org.tron.model.BlockTransactionRequest;
import org.tron.model.BlockTransactionResponse;
import org.tron.model.Error;
import org.tron.model.OperationIdentifier;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class BlockApiController implements BlockApi {
  @Autowired
  private Wallet wallet;

  @Autowired
  private ChainBaseManager chainBaseManager;

  private final NativeWebRequest request;

  @org.springframework.beans.factory.annotation.Autowired
  public BlockApiController(NativeWebRequest request) {
    this.request = request;
  }

  @Override
  public Optional<NativeWebRequest> getRequest() {
    return Optional.ofNullable(request);
  }


  /**
   * POST /block : Get a Block
   * Get a block by its Block Identifier. If transactions are returned in the same call to the node as fetching the block, the response should include these transactions in the Block object. If not, an array of Transaction Identifiers should be returned so /block/transaction fetches can be done to get all transaction information.
   *
   * @param blockRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Get a Block", nickname = "block", notes = "Get a block by its Block Identifier. If transactions are returned in the same call to the node as fetching the block, the response should include these transactions in the Block object. If not, an array of Transaction Identifiers should be returned so /block/transaction fetches can be done to get all transaction information.", response = BlockResponse.class, tags = {"Block",})
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Expected response to a valid request", response = BlockResponse.class),
      @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/block",
      produces = {"application/json"},
      consumes = {"application/json"},
      method = RequestMethod.POST)
  public ResponseEntity<BlockResponse> block(@ApiParam(value = "", required = true) @Valid @RequestBody BlockRequest blockRequest) {
    AtomicInteger statusCode = new AtomicInteger(200);

    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          BlockResponse blockResponse = new BlockResponse();
          String returnString = "";
          Error error = new Error();

          try {
            Long blockIndex = blockRequest.getBlockIdentifier().getIndex();
            String blockHash = blockRequest.getBlockIdentifier().getHash();

            org.tron.model.Block rstBlock = new org.tron.model.Block();
            BlockCapsule tronBlock = null;
            BlockCapsule tronBlockParent = null;

            if (null != blockIndex) {
              tronBlock = chainBaseManager.getBlockByNum(blockIndex);
            } else if (null != blockHash) {
              tronBlock = chainBaseManager.getBlockStore().get(ByteString.copyFrom(ByteArray.fromHexString(blockHash)).toByteArray());
            } else {
              tronBlock = chainBaseManager.getBlockStore().getBlockByLatestNum(1).get(0);
            }
            if (null != tronBlock && tronBlock.getNum() > 0) {
              tronBlockParent = chainBaseManager.getBlockById(tronBlock.getParentHash());
            } else {
              tronBlockParent = tronBlock;
            }

            rstBlock.setBlockIdentifier(
                new BlockIdentifier()
                    .index(tronBlock.getNum())
                    .hash(ByteArray.toHexString(tronBlock.getBlockId().getBytes())));
            rstBlock.setParentBlockIdentifier(
                new BlockIdentifier()
                    .index(tronBlockParent.getNum())
                    .hash(ByteArray.toHexString(tronBlockParent.getBlockId().getBytes())));
            rstBlock.setTimestamp(tronBlock.getTimeStamp());

            List<TransactionCapsule> tronTxs = tronBlock.getTransactions();
            List<org.tron.model.Transaction> rstTxs = Lists.newArrayList();
            System.out.println("tronTxs.size():" + tronTxs.size());
            for (TransactionCapsule tronTx : tronTxs) {
              String status = "0";
              if (null != tronTx.getContractRet()) {
                status = tronTx.getContractRet().toString();
              }

              rstTxs.add(new org.tron.model.Transaction()
                  .transactionIdentifier(new org.tron.model.TransactionIdentifier()
                      .hash(tronTx.getTransactionId().toString()))
                  .addOperationsItem(new org.tron.model.Operation()
                      .operationIdentifier(new OperationIdentifier().index((long) 1))
                      .type(tronTx.getInstance().getRawData().getContract(0).getType().toString())
                      .status(status)));
            }
            rstBlock.setTransactions(rstTxs);

            blockResponse.setBlock(rstBlock);
            returnString = JSON.toJSONString(blockResponse);
          } catch (java.lang.Error | ItemNotFoundException | BadItemException e) {
            e.printStackTrace();
            statusCode.set(500);
            error.setCode(100);
            error.setMessage("error:" + e.getMessage());
            error.setRetriable(false);
            returnString = JSON.toJSONString(error);
          }
          ApiUtil.setExampleResponse(request, "application/json", returnString);
          break;
        }
      }
    });

    return new ResponseEntity<>(HttpStatus.valueOf(statusCode.get()));
  }


  /**
   * POST /block/transaction : Get a Block Transaction
   * Get a transaction in a block by its Transaction Identifier. This endpoint should only be used when querying a node for a block does not return all transactions contained within it. All transactions returned by this endpoint must be appended to any transactions returned by the /block method by consumers of this data. Fetching a transaction by hash is considered an Explorer Method (which is classified under the Future Work section). Calling this endpoint requires reference to a BlockIdentifier because transaction parsing can change depending on which block contains the transaction. For example, in Bitcoin it is necessary to know which block contains a transaction to determine the destination of fee payments. Without specifying a block identifier, the node would have to infer which block to use (which could change during a re-org). Implementations that require fetching previous transactions to populate the response (ex: Previous UTXOs in Bitcoin) may find it useful to run a cache within the Rosetta server in the /data directory (on a path that does not conflict with the node).
   *
   * @param blockTransactionRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Get a Block Transaction", nickname = "blockTransaction", notes = "Get a transaction in a block by its Transaction Identifier. This endpoint should only be used when querying a node for a block does not return all transactions contained within it. All transactions returned by this endpoint must be appended to any transactions returned by the /block method by consumers of this data. Fetching a transaction by hash is considered an Explorer Method (which is classified under the Future Work section). Calling this endpoint requires reference to a BlockIdentifier because transaction parsing can change depending on which block contains the transaction. For example, in Bitcoin it is necessary to know which block contains a transaction to determine the destination of fee payments. Without specifying a block identifier, the node would have to infer which block to use (which could change during a re-org). Implementations that require fetching previous transactions to populate the response (ex: Previous UTXOs in Bitcoin) may find it useful to run a cache within the Rosetta server in the /data directory (on a path that does not conflict with the node).", response = BlockTransactionResponse.class, tags = {"Block",})
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Expected response to a valid request", response = BlockTransactionResponse.class),
      @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/block/transaction",
      produces = {"application/json"},
      consumes = {"application/json"},
      method = RequestMethod.POST)
  public ResponseEntity<BlockTransactionResponse> blockTransaction(@ApiParam(value = "", required = true) @Valid @RequestBody BlockTransactionRequest blockTransactionRequest) {
    AtomicInteger statusCode = new AtomicInteger(200);

    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          BlockTransactionResponse blockTransactionResponse = new BlockTransactionResponse();
          String returnString = "";
          Error error = new Error();

          try {
            long blockIndex = blockTransactionRequest.getBlockIdentifier().getIndex();
            String txID = blockTransactionRequest.getTransactionIdentifier().getHash();
            BlockCapsule tronBlock = chainBaseManager.getBlockByNum(blockIndex);
            System.out.println("blockIndex:" + blockIndex);

            List<TransactionCapsule> tronTxs = tronBlock.getTransactions();
            for (TransactionCapsule tronTx : tronTxs) {
              if (tronTx.getTransactionId().toString().equals(txID)) {
                String status = "0";
                if (null != tronTx.getContractRet()) {
                  status = tronTx.getContractRet().toString();
                }

                blockTransactionResponse.setTransaction(new org.tron.model.Transaction()
                    .transactionIdentifier(new org.tron.model.TransactionIdentifier()
                        .hash(tronTx.getTransactionId().toString()))
                    .addOperationsItem(new org.tron.model.Operation()
                        .operationIdentifier(new OperationIdentifier().index((long) 1))
                        .type(tronTx.getInstance().getRawData().getContract(0).getType().toString())
                        .status(status)));
                break;
              }
            }

            returnString = JSON.toJSONString(blockTransactionResponse);
          } catch (java.lang.Error | ItemNotFoundException | BadItemException e) {
            e.printStackTrace();
            statusCode.set(500);
            error.setCode(100);
            error.setMessage("error:" + e.getMessage());
            error.setRetriable(false);
            returnString = JSON.toJSONString(error);
          }
          ApiUtil.setExampleResponse(request, "application/json", returnString);
          break;
        }
      }
    });
    return new ResponseEntity<>(HttpStatus.valueOf(statusCode.get()));
  }
}
