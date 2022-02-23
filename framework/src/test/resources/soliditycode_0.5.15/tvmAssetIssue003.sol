pragma solidity ^0.5.12;

contract tvmAssetIssue003 {
    constructor() payable public{}

    function tokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public returns (uint) {
        return assetissue(name, abbr, totalSupply, precision);
    }

    function tokenIssueAndTransfer(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision, address addr) public {
        address payable newaddress = address(uint160(addr));
        newaddress.transfer(100000000);
        assetissue(name, abbr, totalSupply, precision);
        newaddress.transfer(100000000);
    }

    function updateAssetAndTransfer(trcToken tokenId, string memory url, string memory desc, address addr) public {
        address payable newaddress = address(uint160(addr));
        newaddress.transfer(100000000);
        updateasset(tokenId, bytes(url), bytes(desc));
        newaddress.transfer(100000000);
    }
}