package org.tron.api;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import java.util.Optional;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.model.ConstructionDeriveRequest;
import org.tron.model.ConstructionDeriveResponse;
import org.tron.model.CurveType;
import org.tron.model.Error;
import org.tron.model.PublicKey;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class ConstructionApiController implements ConstructionApi {

    private final NativeWebRequest request;

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

}
