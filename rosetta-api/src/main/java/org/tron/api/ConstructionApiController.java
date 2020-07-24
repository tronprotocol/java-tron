package org.tron.api;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.model.*;
import org.tron.model.Error;
import org.tron.protos.Protocol;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class ConstructionApiController implements ConstructionApi {

    private final NativeWebRequest request;

    @Autowired
    private Wallet wallet;

    @org.springframework.beans.factory.annotation.Autowired
    public ConstructionApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    /**
     * POST /construction/combine : Create Network Transaction from Signatures
     * Combine creates a network-specific transaction from an unsigned transaction and an array of provided signatures. The signed transaction returned from this method will be sent to the &#x60;/construction/submit&#x60; endpoint by the caller.
     *
     * @param constructionCombineRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @SuppressWarnings("checkstyle:Indentation")
    @ApiOperation(value = "Create Network Transaction from Signatures", nickname = "constructionCombine", notes = "Combine creates a network-specific transaction from an unsigned transaction and an array of provided signatures. The signed transaction returned from this method will be sent to the `/construction/submit` endpoint by the caller.", response = ConstructionCombineResponse.class, tags={ "Construction", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionCombineResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/construction/combine",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<ConstructionCombineResponse> constructionCombine(@ApiParam(value = "" ,required=true )  @Valid @RequestBody ConstructionCombineRequest constructionCombineRequest) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString;
                    try {
                        Protocol.Transaction.raw transaction = Protocol.Transaction.raw.parseFrom(constructionCombineRequest.getUnsignedTransaction().getBytes());
                        Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
                        transactionBuilder.setRawData(transaction);
                        for (Signature signature:constructionCombineRequest.getSignatures()) {
                            transactionBuilder.addSignature(ByteString.copyFrom(signature.getHexBytes().getBytes()));
                        }
                        exampleString = "{ \"signed_transaction\" : "+ transactionBuilder.build().toString() + " }";
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        exampleString = "{ \"signed_transaction\" : \"signed_transaction\" }";
                    }

                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.valueOf(200));

    }

    /**
     * POST /construction/hash : Get the Hash of a Signed Transaction
     * TransactionHash returns the network-specific transaction hash for a signed transaction.
     *
     * @param constructionHashRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get the Hash of a Signed Transaction", nickname = "constructionHash", notes = "TransactionHash returns the network-specific transaction hash for a signed transaction.", response = ConstructionHashResponse.class, tags={ "Construction", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionHashResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/construction/hash",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<ConstructionHashResponse> constructionHash(@ApiParam(value = "" ,required=true )  @Valid @RequestBody ConstructionHashRequest constructionHashRequest) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    Protocol.Transaction transaction = null;
                    try {
                        transaction = Protocol.Transaction.parseFrom(constructionHashRequest.getSignedTransaction().getBytes());
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }

                    String exampleString = "{ \"transaction_hash\" : \"transaction_hash\" }";
                    if (transaction != null) {
                        String transactionHash = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).toString();
                        exampleString = "{ \"transaction_hash\" : " + transactionHash + " }";
                    }

                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.valueOf(200));

    }

    /**
     * POST /construction/submit : Submit a Signed Transaction
     * Submit a pre-signed transaction to the node. This call should not block on the transaction being included in a block. Rather, it should return immediately with an indication of whether or not the transaction was included in the mempool. The transaction submission response should only return a 200 status if the submitted transaction could be included in the mempool. Otherwise, it should return an error.
     *
     * @param constructionSubmitRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Submit a Signed Transaction", nickname = "constructionSubmit", notes = "Submit a pre-signed transaction to the node. This call should not block on the transaction being included in a block. Rather, it should return immediately with an indication of whether or not the transaction was included in the mempool. The transaction submission response should only return a 200 status if the submitted transaction could be included in the mempool. Otherwise, it should return an error.", response = ConstructionSubmitResponse.class, tags={ "Construction", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionSubmitResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/construction/submit",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<ConstructionSubmitResponse> constructionSubmit(@ApiParam(value = "" ,required=true )  @Valid @RequestBody ConstructionSubmitRequest constructionSubmitRequest) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    Protocol.Transaction transactionSigned = null;
                    try {
                        transactionSigned = Protocol.Transaction.parseFrom(ByteArray.fromHexString(constructionSubmitRequest.getSignedTransaction()));
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                    String exampleString = "{ \"metadata\" : \"{}\", \"transaction_identifier\" : { \"hash\" : \"0x2f23fd8cca835af21f3ac375bac601f97ead75f2e79143bdf71fe2c4be043e8f\" } }";
                    if (transactionSigned != null) {
                        wallet.broadcastTransaction(transactionSigned);
                        String transactionHash = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), transactionSigned.getRawData().toByteArray()).toString();
                        exampleString = "{ { \"metadata\" : \"{}\", \"transaction_identifier\" : { \"hash\" : " + transactionHash + " } }";
                    }
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.valueOf(200));

    }
}
