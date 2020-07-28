package org.tron.api;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.Valid;

import org.joda.time.DateTime;
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
import org.tron.protos.contract.BalanceContract;


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
   * Combine creates a network-specific transaction from an unsigned transaction and
   * an array of provided signatures. The signed transaction returned from this method
   * will be sent to the &#x60;/construction/submit&#x60; endpoint by the caller.
   *
   * @param constructionCombineRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @SuppressWarnings("checkstyle:Indentation")
  @ApiOperation(value = "Create Network Transaction from Signatures", nickname = "constructionCombine", notes = "Combine creates a network-specific transaction from an unsigned transaction and an array of provided signatures. The signed transaction returned from this method will be sent to the `/construction/submit` endpoint by the caller.", response = ConstructionCombineResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionCombineResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/combine",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionCombineResponse> constructionCombine(@ApiParam(value = "", required = true) @Valid @RequestBody ConstructionCombineRequest constructionCombineRequest) {
    AtomicInteger statusCode = new AtomicInteger(200);
    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String returnString;
//          BalanceContract.TransferContract transferContract = BalanceContract.TransferContract.newBuilder().setAmount(10)
//                  .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
//                  .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf"))).build();
//          Protocol.Transaction transactionTest = Protocol.Transaction.newBuilder().setRawData(
//                  Protocol.Transaction.raw.newBuilder().setTimestamp(DateTime.now().minusDays(4).getMillis())
//                          .setRefBlockNum(1)
//                          .addContract(
//                                  Protocol.Transaction.Contract.newBuilder().setType(Protocol.Transaction.Contract.ContractType.TransferContract)
//                                          .setParameter(Any.pack(transferContract)).build()).build())
//                  .build();
//          ConstructionCombineResponse constructionCombineResponse = new ConstructionCombineResponse();
//          constructionCombineResponse.setSignedTransaction(Base64.getEncoder().encodeToString(transactionTest.getRawData().toByteArray()));
//          returnString = JSON.toJSONString(constructionCombineResponse);
//          constructionCombineRequest.setUnsignedTransaction(Base64.getEncoder().encodeToString(transactionTest.getRawData().toByteArray()));
          try {
            Protocol.Transaction.raw transaction = Protocol.Transaction.raw.parseFrom(Base64.getDecoder().decode(constructionCombineRequest.getUnsignedTransaction()));
            Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
            transactionBuilder.setRawData(transaction);
            for (Signature signature : constructionCombineRequest.getSignatures()) {
              transactionBuilder.addSignature(ByteString.copyFrom(signature.getHexBytes().getBytes()));
            }
            ConstructionCombineResponse constructionCombineResponse = new ConstructionCombineResponse();
            constructionCombineResponse.setSignedTransaction(Base64.getEncoder().encodeToString(transactionBuilder.build().toByteArray()));
            returnString = JSON.toJSONString(constructionCombineResponse);
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            statusCode.set(500);
            Error error = new Error();
            error.setCode(100);
            error.setMessage("Invalid unsigned transaction format");
            error.setRetriable(false);
            returnString = JSON.toJSONString(error);
          }

          ApiUtil.setExampleResponse(request, "application/json", returnString);
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
   * @param constructionHashRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Get the Hash of a Signed Transaction", nickname = "constructionHash", notes = "TransactionHash returns the network-specific transaction hash for a signed transaction.", response = ConstructionHashResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionHashResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/hash",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionHashResponse> constructionHash(@ApiParam(value = "", required = true) @Valid @RequestBody ConstructionHashRequest constructionHashRequest) {
    AtomicInteger statusCode = new AtomicInteger(200);
    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          Protocol.Transaction transaction = null;
          String returnString = "{ \"transaction_hash\" : \"transaction_hash\" }";
          try {
            transaction = Protocol.Transaction.parseFrom(
                    Base64.getDecoder().decode(constructionHashRequest.getSignedTransaction()));
            String transactionHash = Base64.getEncoder().encodeToString(Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
            ConstructionHashResponse constructionHashResponse = new ConstructionHashResponse();
            constructionHashResponse.setTransactionHash(transactionHash);
            returnString = JSON.toJSONString(constructionHashResponse);
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            statusCode.set(500);
            Error error = new Error();
            error.setCode(100);
            error.setMessage("Invalid transaction format");
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
   * POST /construction/submit : Submit a Signed Transaction
   * Submit a pre-signed transaction to the node.
   * This call should not block on the transaction being included in a block.
   * Rather, it should return immediately with an indication of whether or not
   * the transaction was included in the mempool. The transaction submission
   * response should only return a 200 status if the submitted transaction could
   * be included in the mempool. Otherwise, it should return an error.
   *
   * @param constructionSubmitRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Submit a Signed Transaction", nickname = "constructionSubmit", notes = "Submit a pre-signed transaction to the node. This call should not block on the transaction being included in a block. Rather, it should return immediately with an indication of whether or not the transaction was included in the mempool. The transaction submission response should only return a 200 status if the submitted transaction could be included in the mempool. Otherwise, it should return an error.", response = ConstructionSubmitResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request",
                  response = ConstructionSubmitResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/submit",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionSubmitResponse> constructionSubmit(
          @ApiParam(value = "", required = true) @Valid @RequestBody ConstructionSubmitRequest constructionSubmitRequest) {
    AtomicInteger statusCode = new AtomicInteger(200);

    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String returnString = "";
          Error error = new Error();
          try {
            Protocol.Transaction transactionSigned = Protocol.Transaction.parseFrom(
                    Base64.getDecoder().decode(constructionSubmitRequest.getSignedTransaction()));
            GrpcAPI.Return result = wallet.broadcastTransaction(transactionSigned);
            if (result.getResult()) {
              String transactionHash = Sha256Hash.hash(
                      CommonParameter.getInstance().isECKeyCryptoEngine(),
                      transactionSigned.getRawData().toByteArray()).toString();
              ConstructionSubmitResponse constructionSubmitResponse = new ConstructionSubmitResponse();
              constructionSubmitResponse.getTransactionIdentifier().setHash(transactionHash);
              returnString = JSON.toJSONString(constructionSubmitResponse);
            } else {
              statusCode.set(500);
              error.setCode(result.getCodeValue());
              error.setMessage(result.getMessage().toString());
              error.setRetriable(true);
              returnString = JSON.toJSONString(error);
            }
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            statusCode.set(500);
            error.setCode(100);
            error.setMessage("Invalid transaction format");
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
