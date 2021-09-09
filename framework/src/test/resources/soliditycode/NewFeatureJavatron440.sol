contract C {
    constructor() public payable{}

    function baseFee() external view returns (uint ret) {
        assembly {
            ret := basefee()
        }
        assert(block.basefee == ret);
    }

    function baseFeeOnly() external view returns (uint ret) {
        assembly {
            ret := basefee()
        }
    }

    function gasPrice() external view returns (uint ret) {
        assembly {
            ret := basefee()
        }
        assert(tx.gasprice == ret);
    }

    function gasPriceOnly() external view returns (uint) {
        return tx.gasprice;
    }

    function testCall(address payable caller, address payable transferTo) public {
        (bool success, bytes memory data) = caller.call(abi.encodeWithSignature("transfer(address)",transferTo));
        require(success);
    }

}

contract D {
    constructor() public payable{}

    function deploy(uint256 salt) public returns(address){
        address addr;
        bytes memory code = type(C).creationCode;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
        }
        return addr;
    }
    uint sum = 0;
    function transfer(address payable transerTo) public {
        for (uint i = 0; i < type(uint256).max; i++)
            sum = 0;
            for (uint j = 0; j < type(uint8).max; j++)
                sum += j;
            transerTo.transfer(1);
    }
}





