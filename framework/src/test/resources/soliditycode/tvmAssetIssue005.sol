

contract tvmAssetIssue005 {
    constructor() payable public{}

    fallback() external payable {
    }

    function tokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public returns (uint) {
        return assetissue(name, abbr, totalSupply, precision);
    }

    function updateAsset(trcToken tokenId, string memory url, string memory desc) public returns (bool) {
        return updateasset(tokenId, bytes(url), bytes(desc));
    }

    function updateAssetOnBytes(trcToken tokenId, bytes memory url, bytes memory desc) public returns (bool) {
        return updateasset(tokenId, url, desc);
    }

    function transferToken(address payable toAddress, uint256 tokenValue, trcToken id) payable public {
        toAddress.transferToken(tokenValue, id);
    }

    function SelfdestructTest(address payable target) public {
        selfdestruct(target);
    }
}

contract B {
    event Deployed(address addr, uint256 salt);

    function deploy(uint256 salt) public returns (address) {
        address addr;
        bytes memory code = type(tvmAssetIssue005).creationCode;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        emit Deployed(addr, salt);
        return addr;
    }
}