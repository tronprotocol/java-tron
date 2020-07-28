package org.tron.api;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
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
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.model.*;
import org.tron.model.Error;

import org.tron.core.Wallet;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
     * @param blockRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get a Block", nickname = "block", notes = "Get a block by its Block Identifier. If transactions are returned in the same call to the node as fetching the block, the response should include these transactions in the Block object. If not, an array of Transaction Identifiers should be returned so /block/transaction fetches can be done to get all transaction information.", response = BlockResponse.class, tags={ "Block", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = BlockResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/block",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<BlockResponse> block(@ApiParam(value = "" ,required=true )  @Valid @RequestBody BlockRequest blockRequest) {
        AtomicInteger statusCode = new AtomicInteger(200);

        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    BlockResponse blockResponse = new BlockResponse();
                    String returnString = "";
                    Error error = new Error();

                    try{
                        long blockIndex = blockRequest.getBlockIdentifier().getIndex();
                        String blockHash = blockRequest.getBlockIdentifier().getHash();
                        System.out.println("blockIndex:"+blockIndex+",blockHash:"+blockHash);

                        org.tron.model.Block rstBlock = new org.tron.model.Block();
                        BlockCapsule tronBlock;
                        BlockCapsule tronBlockParent;

                        if(0 != blockIndex){
                            tronBlock = chainBaseManager.getBlockByNum(blockIndex);
                        }else if(null != blockHash){
                            tronBlock = chainBaseManager.getBlockById(Sha256Hash.wrap(blockHash.getBytes()));
                        }else{
                            tronBlock = chainBaseManager.getBlockStore().getBlockByLatestNum(1).get(0);
                        }
                        tronBlockParent = chainBaseManager.getBlockById(tronBlock.getParentHash());

                        rstBlock.setBlockIdentifier(
                                new BlockIdentifier()
                                        .index(tronBlock.getNum())
                                        .hash(tronBlock.getBlockId().getString()));
                        rstBlock.setParentBlockIdentifier(
                                new BlockIdentifier()
                                        .index(tronBlockParent.getNum())
                                        .hash(tronBlockParent.getBlockId().getString()));
                        rstBlock.setTimestamp(new Timestamp(tronBlock.getTimeStamp()));

                        List<TransactionCapsule> tronTxs = tronBlock.getTransactions();
                        List<org.tron.model.Transaction> rstTxs = Lists.newArrayList();
                        tronTxs.forEach(tronTx -> {
                            rstTxs.add(new org.tron.model.Transaction()
                                    .transactionIdentifier(new org.tron.model.TransactionIdentifier()
                                            .hash(tronTx.getTransactionId().toString()))
                                    .addOperationsItem(new org.tron.model.Operation()
                                            .operationIdentifier(new OperationIdentifier().index((long)1))
                                            .type(tronTx.getInstance().getRawData().getContract(0).getType().toString())
                                            .status(tronTx.getContractRet().toString())));
                        });
                        rstBlock.setTransactions(rstTxs);

                        blockResponse.setBlock(rstBlock);
                        returnString = JSON.toJSONString(blockResponse);
                    }catch (java.lang.Error | ItemNotFoundException | BadItemException e){
                        System.out.println("error:"+e.getMessage());
                        e.printStackTrace();
                        statusCode.set(500);
                        error.setCode(100);
                        error.setMessage("error in server");
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
     * @param blockTransactionRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get a Block Transaction", nickname = "blockTransaction", notes = "Get a transaction in a block by its Transaction Identifier. This endpoint should only be used when querying a node for a block does not return all transactions contained within it. All transactions returned by this endpoint must be appended to any transactions returned by the /block method by consumers of this data. Fetching a transaction by hash is considered an Explorer Method (which is classified under the Future Work section). Calling this endpoint requires reference to a BlockIdentifier because transaction parsing can change depending on which block contains the transaction. For example, in Bitcoin it is necessary to know which block contains a transaction to determine the destination of fee payments. Without specifying a block identifier, the node would have to infer which block to use (which could change during a re-org). Implementations that require fetching previous transactions to populate the response (ex: Previous UTXOs in Bitcoin) may find it useful to run a cache within the Rosetta server in the /data directory (on a path that does not conflict with the node).", response = BlockTransactionResponse.class, tags={ "Block", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = BlockTransactionResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/block/transaction",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<BlockTransactionResponse> blockTransaction(@ApiParam(value = "" ,required=true )  @Valid @RequestBody BlockTransactionRequest blockTransactionRequest) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"transaction\" : { \"metadata\" : { \"size\" : 12378, \"lockTime\" : 1582272577 }, \"operations\" : [ { \"amount\" : { \"metadata\" : \"{}\", \"currency\" : { \"symbol\" : \"BTC\", \"metadata\" : { \"Issuer\" : \"Satoshi\" }, \"decimals\" : 8 }, \"value\" : \"1238089899992\" }, \"metadata\" : { \"asm\" : \"304502201fd8abb11443f8b1b9a04e0495e0543d05611473a790c8939f089d073f90509a022100f4677825136605d732e2126d09a2d38c20c75946cd9fc239c0497e84c634e3dd01 03301a8259a12e35694cc22ebc45fee635f4993064190f6ce96e7fb19a03bb6be2\", \"hex\" : \"48304502201fd8abb11443f8b1b9a04e0495e0543d05611473a790c8939f089d073f90509a022100f4677825136605d732e2126d09a2d38c20c75946cd9fc239c0497e84c634e3dd012103301a8259a12e35694cc22ebc45fee635f4993064190f6ce96e7fb19a03bb6be2\" }, \"related_operations\" : [ { \"index\" : 0, \"operation_identifier\" : { \"index\" : 0 } } ], \"type\" : \"Transfer\", \"account\" : { \"metadata\" : \"{}\", \"address\" : \"0x3a065000ab4183c6bf581dc1e55a605455fc6d61\", \"sub_account\" : { \"metadata\" : \"{}\", \"address\" : \"0x6b175474e89094c44da98b954eedeac495271d0f\" } }, \"operation_identifier\" : { \"index\" : 1, \"network_index\" : 0 }, \"status\" : \"Reverted\" }, { \"amount\" : { \"metadata\" : \"{}\", \"currency\" : { \"symbol\" : \"BTC\", \"metadata\" : { \"Issuer\" : \"Satoshi\" }, \"decimals\" : 8 }, \"value\" : \"1238089899992\" }, \"metadata\" : { \"asm\" : \"304502201fd8abb11443f8b1b9a04e0495e0543d05611473a790c8939f089d073f90509a022100f4677825136605d732e2126d09a2d38c20c75946cd9fc239c0497e84c634e3dd01 03301a8259a12e35694cc22ebc45fee635f4993064190f6ce96e7fb19a03bb6be2\", \"hex\" : \"48304502201fd8abb11443f8b1b9a04e0495e0543d05611473a790c8939f089d073f90509a022100f4677825136605d732e2126d09a2d38c20c75946cd9fc239c0497e84c634e3dd012103301a8259a12e35694cc22ebc45fee635f4993064190f6ce96e7fb19a03bb6be2\" }, \"related_operations\" : [ { \"index\" : 0, \"operation_identifier\" : { \"index\" : 0 } } ], \"type\" : \"Transfer\", \"account\" : { \"metadata\" : \"{}\", \"address\" : \"0x3a065000ab4183c6bf581dc1e55a605455fc6d61\", \"sub_account\" : { \"metadata\" : \"{}\", \"address\" : \"0x6b175474e89094c44da98b954eedeac495271d0f\" } }, \"operation_identifier\" : { \"index\" : 1, \"network_index\" : 0 }, \"status\" : \"Reverted\" } ], \"transaction_identifier\" : { \"hash\" : \"0x2f23fd8cca835af21f3ac375bac601f97ead75f2e79143bdf71fe2c4be043e8f\" } } }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.valueOf(200));

    }
}
