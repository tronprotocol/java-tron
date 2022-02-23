pragma solidity ^0.5.12;

contract tvmAssetIssue002 {
    constructor() payable public{}

    function tokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public returns (uint) {
        assetissue(name, abbr, totalSupply, precision);
        return assetissue(name, abbr, totalSupply, precision);
    }

    function updateAsset(trcToken tokenId, string memory url1, string memory desc1, string memory url2, string memory desc2) public returns (bool) {
        updateasset(tokenId, bytes(url1), bytes(desc1));
        return updateasset(tokenId, bytes(url2), bytes(desc2));
    }
}