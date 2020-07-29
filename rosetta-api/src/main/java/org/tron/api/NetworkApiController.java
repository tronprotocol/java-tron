package org.tron.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.util.Optional;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.utils.ByteArray;
import org.tron.config.Constant;
import org.tron.core.ChainBaseManager;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.model.Allow;
import org.tron.model.BlockIdentifier;
import org.tron.model.Error;
import org.tron.model.MetadataRequest;
import org.tron.model.NetworkIdentifier;
import org.tron.model.NetworkListResponse;
import org.tron.model.NetworkOptionsResponse;
import org.tron.model.NetworkRequest;
import org.tron.model.NetworkStatusResponse;
import org.tron.model.OperationStatus;
import org.tron.model.Peer;
import org.tron.model.Version;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class NetworkApiController implements NetworkApi {

    @Autowired
    private ChainBaseManager chainBaseManager;

    @Autowired
    private ChannelManager channelManager;

    private final NativeWebRequest request;

    public NetworkApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    /**
     * POST /network/list : Get List of Available Networks
     * This endpoint returns a list of NetworkIdentifiers that the Rosetta server supports.
     *
     * @param metadataRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get List of Available Networks", nickname = "networkList", notes = "This endpoint returns a list of NetworkIdentifiers that the Rosetta server supports.", response = NetworkListResponse.class, tags={ "Network", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = NetworkListResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/network/list",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<NetworkListResponse> networkList(@ApiParam(value = "" ,required=true )  @Valid @RequestBody MetadataRequest metadataRequest) {
        NetworkListResponse networkListResponse = new NetworkListResponse();
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    NetworkIdentifier networkIdentifier = new NetworkIdentifier();
                    networkIdentifier.setBlockchain("tron");
                    networkIdentifier.setNetwork("mainnet");
                    networkListResponse.addNetworkIdentifiersItem(networkIdentifier);
                    break;
                }
            }
        });
        return new ResponseEntity<>(networkListResponse, HttpStatus.OK);

    }

    /**
     * POST /network/options : Get Network Options
     * This endpoint returns the version information and allowed network-specific types for a NetworkIdentifier. Any NetworkIdentifier returned by /network/list should be accessible here. Because options are retrievable in the context of a NetworkIdentifier, it is possible to define unique options for each network.
     *
     * @param networkRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get Network Options", nickname = "networkOptions", notes = "This endpoint returns the version information and allowed network-specific types for a NetworkIdentifier. Any NetworkIdentifier returned by /network/list should be accessible here. Because options are retrievable in the context of a NetworkIdentifier, it is possible to define unique options for each network.", response = NetworkOptionsResponse.class, tags={ "Network", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = NetworkOptionsResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/network/options",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<NetworkOptionsResponse> networkOptions(@ApiParam(value = "" ,required=true )  @Valid @RequestBody NetworkRequest networkRequest) {

        NetworkOptionsResponse networkOptionsResponse = new NetworkOptionsResponse();
        networkOptionsResponse.setVersion(
                new Version()
                        .rosettaVersion(Constant.rosettaVersion)
                        .nodeVersion(org.tron.program.Version.getVersion())
                        .middlewareVersion(Constant.middlewareVersion)
        );
        Allow allow = new Allow();
        allow.setHistoricalBalanceLookup(false);
        allow.setOperationTypes(Arrays.asList(Constant.supportOperationTypes));
        allow.setOperationStatuses(Constant.supportOperationStatuses);
        allow.setErrors(Constant.supportErrors);

        networkOptionsResponse.setAllow(allow);
        return new ResponseEntity<>(networkOptionsResponse, HttpStatus.OK);
    }


    /**
     * POST /network/status : Get Network Status
     * This endpoint returns the current status of the network requested. Any NetworkIdentifier returned by /network/list should be accessible here.
     *
     * @param networkRequest  (required)
     * @return Expected response to a valid request (status code 200)
     *         or unexpected error (status code 200)
     */
    @ApiOperation(value = "Get Network Status", nickname = "networkStatus", notes = "This endpoint returns the current status of the network requested. Any NetworkIdentifier returned by /network/list should be accessible here.", response = NetworkStatusResponse.class, tags={ "Network", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Expected response to a valid request", response = NetworkStatusResponse.class),
            @ApiResponse(code = 200, message = "unexpected error", response = Error.class) })
    @RequestMapping(value = "/network/status",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<NetworkStatusResponse> networkStatus(@ApiParam(value = "" ,required=true )  @Valid @RequestBody NetworkRequest networkRequest) {
        NetworkStatusResponse networkStatusResponse = new NetworkStatusResponse();
        BlockCapsule currentBlock = chainBaseManager.getBlockStore().getBlockByLatestNum(1).get(0);
        networkStatusResponse.setCurrentBlockIdentifier(
                new BlockIdentifier()
                        .index(currentBlock.getNum())
                        .hash(ByteArray.toHexString(currentBlock.getBlockId().getBytes())));

        BlockCapsule genesisBlock = chainBaseManager.getGenesisBlock();
        networkStatusResponse.setGenesisBlockIdentifier(
                new BlockIdentifier()
                        .index(genesisBlock.getNum())
                        .hash(ByteArray.toHexString(genesisBlock.getBlockId().getBytes())));

        networkStatusResponse.setOldestBlockIdentifier(
                new BlockIdentifier()
                        .index(currentBlock.getNum())
                        .hash(ByteArray.toHexString(currentBlock.getBlockId().getBytes())));

//        networkStatusResponse.setCurrentBlockTimestamp(new Timestamp(currentBlock.getTimeStamp()));
        networkStatusResponse.setCurrentBlockTimestamp(currentBlock.getTimeStamp());

        List<Channel> activePeers = new ArrayList<>(channelManager.getActivePeers());
        List<Peer> peers = Lists.newArrayList();
        activePeers.forEach(peer -> {
            Map<String, Object> metaData = Maps.newHashMap();
            metaData.put("address", peer.getNode().getHost());
            metaData.put("port", peer.getNode().getPort());
            peers.add(new Peer()
                    .peerId(peer.getPeerId())
                    .metadata(metaData));
        });
        networkStatusResponse.setPeers(peers);

        return new ResponseEntity<>(networkStatusResponse, HttpStatus.OK);
    }

    private boolean checkRequest(NetworkRequest networkRequest) {

        return true;
    }

}
