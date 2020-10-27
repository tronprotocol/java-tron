pragma solidity ^0.5.12;

contract A {

    constructor() payable public{}
    function() payable external {}

    function tokenIssueA(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public returns (uint){
        return assetissue(name, abbr, totalSupply, precision);
    }

    function updateAssetA(trcToken tokenId, string memory url, string memory desc) public returns (bool) {
        return updateasset(tokenId, bytes(url), bytes(desc));
    }

    function transferToken(address payable toAddress, uint256 tokenValue, trcToken id) payable public {
        toAddress.transferToken(tokenValue, id);
    }
}

contract tvmAssetIssue004 {

    A a;

    constructor() payable public{}

    function tokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public returns (uint) {
        return a.tokenIssueA(name, abbr, totalSupply, precision);
    }

    function updateAsset(trcToken tokenId, string memory url, string memory desc) public returns (bool) {
        return a.updateAssetA(tokenId, url, desc);
    }

    function getContractAddress() public payable returns (address) {
        a = (new A).value(1024000000)();
        return address(a);
    }
}