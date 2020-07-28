package org.tron.api;

import com.alibaba.fastjson.JSONObject;
import com.google.common.primitives.Ints;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.model.ConstructionDeriveRequest;
import org.tron.model.ConstructionDeriveResponse;
import org.tron.model.ConstructionMetadataRequest;
import org.tron.model.ConstructionMetadataResponse;
import org.tron.model.ConstructionPreprocessRequest;
import org.tron.model.ConstructionPreprocessResponse;
import org.tron.model.CurveType;
import org.tron.model.Error;
import org.tron.model.PublicKey;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class ConstructionApiController implements ConstructionApi {

    private final NativeWebRequest request;

    @Autowired
    private DynamicPropertiesStore dynamicPropertiesStore;

    @org.springframework.beans.factory.annotation.Autowired
    public ConstructionApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }


    /**
     * POST /construction/derive : Derive an Address from a PublicKey
     * Derive returns the network-specific address associated with a public key. Blockchains that require an on-chain action to create an account should not implement this method.
     *
     * @param constructionDeriveRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Derive an Address from a PublicKey", nickname = "constructionDerive", notes = "Derive returns the network-specific address associated with a public key. Blockchains that require an on-chain action to create an account should not implement this method.", response = ConstructionDeriveResponse.class, tags={ "Construction", })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionDeriveResponse.class),
        @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/construction/derive",
        produces = { "application/json" },
        consumes = { "application/json" },
        method = RequestMethod.POST)
    public ResponseEntity<ConstructionDeriveResponse> constructionDerive(
        @ApiParam(value = "" ,required=true )
        @Valid
        @RequestBody
            ConstructionDeriveRequest constructionDeriveRequest) {

        if (getRequest().isPresent()) {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    PublicKey publicKey = constructionDeriveRequest.getPublicKey();
                    if (CurveType.SECP256K1.equals(publicKey.getCurveType())) {
                        String hexBytes = publicKey.getHexBytes();
                        byte[] address = Hash.computeAddress(ByteArray.fromHexString(hexBytes));
                        ConstructionDeriveResponse response = new ConstructionDeriveResponse();
                        response.address(StringUtil.encode58Check(address));
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                    break;
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.valueOf(200));
    }

    /**
     * POST /construction/preprocess : Create a Request to Fetch Metadata
     * Preprocess is called prior to &#x60;/construction/payloads&#x60; to construct a request for any metadata that is needed for transaction construction given (i.e. account nonce). The request returned from this method will be used by the caller (in a different execution environment) to call the &#x60;/construction/metadata&#x60; endpoint.
     *
     * @param constructionPreprocessRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Create a Request to Fetch Metadata", nickname = "constructionPreprocess", notes = "Preprocess is called prior to `/construction/payloads` to construct a request for any metadata that is needed for transaction construction given (i.e. account nonce). The request returned from this method will be used by the caller (in a different execution environment) to call the `/construction/metadata` endpoint.", response = ConstructionPreprocessResponse.class, tags={ "Construction", })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionPreprocessResponse.class),
        @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/construction/preprocess",
        produces = { "application/json" },
        consumes = { "application/json" },
        method = RequestMethod.POST)
    public ResponseEntity<ConstructionPreprocessResponse> constructionPreprocess(@ApiParam(value = "" ,required=true )  @Valid @RequestBody ConstructionPreprocessRequest constructionPreprocessRequest) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"options\" : \"{}\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.valueOf(200));

    }

    /**
     * POST /construction/metadata : Get Metadata for Transaction Construction
     * Get any information required to construct a transaction for a specific network. Metadata returned here could be a recent hash to use, an account sequence number, or even arbitrary chain state. The request used when calling this endpoint is often created by calling &#x60;/construction/preprocess&#x60; in an offline environment. It is important to clarify that this endpoint should not pre-construct any transactions for the client (this should happen in &#x60;/construction/payloads&#x60;). This endpoint is left purposely unstructured because of the wide scope of metadata that could be required.
     *
     * @param constructionMetadataRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get Metadata for Transaction Construction", nickname = "constructionMetadata", notes = "Get any information required to construct a transaction for a specific network. Metadata returned here could be a recent hash to use, an account sequence number, or even arbitrary chain state. The request used when calling this endpoint is often created by calling `/construction/preprocess` in an offline environment. It is important to clarify that this endpoint should not pre-construct any transactions for the client (this should happen in `/construction/payloads`). This endpoint is left purposely unstructured because of the wide scope of metadata that could be required.", response = ConstructionMetadataResponse.class, tags={ "Construction", })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionMetadataResponse.class),
        @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/construction/metadata",
        produces = { "application/json" },
        consumes = { "application/json" },
        method = RequestMethod.POST)
    public ResponseEntity<ConstructionMetadataResponse> constructionMetadata(@ApiParam(value = "" ,required=true )  @Valid @RequestBody ConstructionMetadataRequest constructionMetadataRequest) {
        if (getRequest().isPresent()) {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"metadata\" : { \"account_sequence\" : 23, \"recent_block_hash\" : \"0x52bc44d5378309ee2abf1539bf71de1b7d7be3b5\" } }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(dynamicPropertiesStore.getLatestBlockHeaderHash());
                    byte[] referenceBlockNumBytes = ByteArray.subArray(ByteArray.fromLong(blockId.getNum()), 6, 8);
                    int referenceBlockNum = Ints.fromBytes((byte) 0, (byte) 0, referenceBlockNumBytes[0], referenceBlockNumBytes[1]);
                    String referenceBlockHash = ByteArray.toHexString(ByteArray.subArray(blockId.getBytes(), 8, 16));
                    long expiration = dynamicPropertiesStore.getLatestBlockHeaderTimestamp() + Args.getInstance()
                        .getTrxExpirationTimeInMilliseconds();
                    long timestamp = System.currentTimeMillis();

                    Map<String, Object> metadatas = new HashMap<>();
                    metadatas.put("reference_block_num", referenceBlockNum);
                    metadatas.put("reference_block_hash", referenceBlockHash);
                    metadatas.put("expiration", expiration);
                    metadatas.put("timestamp", timestamp);
                    JSONObject jsonObject = new JSONObject(metadatas);
                    ConstructionMetadataResponse response = new ConstructionMetadataResponse();
                    response.setMetadata(jsonObject.toString());

                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
        }

        return new ResponseEntity<>(HttpStatus.valueOf(200));

    }

}
