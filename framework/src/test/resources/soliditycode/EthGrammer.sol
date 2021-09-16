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

    function testDelegateCall(address payable caller, address payable transferTo) public {
        (bool success, bytes memory data) = caller.delegatecall(abi.encodeWithSignature("transfer(address)",transferTo));
        require(success);
    }

    uint sum = 0;
    function transfer(address payable transerTo) public {
        for (uint i = 0; i < type(uint256).max; i++)
            sum = 0;
        for (uint j = 0; j < type(uint8).max; j++)
            sum += j;
        transerTo.transfer(1);
    }

    function testCallFunctionInContract(address payable transferTo) public {
        this.transfer(transferTo);
    }


    function getRipemd160(string memory input) public returns(bytes32 output) {
         bytes memory tem = bytes(input);
         assembly {
           if iszero(staticcall(not(0), 0x020003, add(tem, 0x20), mload(tem), output, 0x20)) {
                 revert(0, 0)
             }
           output := mload(add(output,0x0c))
         }
    }

   function getRipemd160Str(string memory input) public view returns(bytes32 output) {
        assembly {
            if iszero(staticcall(not(0), 0x020003, add(input, 0x20), mload(input), output, 0x20)) {
                revert(0, 0)
            }
            output := mload(add(output,0x0c))
        }

   }

    function F(uint32 rounds, bytes32[2] memory h, bytes32[4] memory m, bytes8[2] memory t, bool f) public view returns (bytes32[2] memory) {
        bytes32[2] memory output;

        bytes memory args = abi.encodePacked(rounds, h[0], h[1], m[0], m[1], m[2], m[3], t[0], t[1], f);

        assembly {
            if iszero(staticcall(not(0), 0x020009, add(args, 32), 0xd5, output, 0x40)) {
                revert(0, 0)
            }
        }

        return output;
    }

    function callF() public view returns (bytes32[2] memory) {
        uint32 rounds = 12;

        bytes32[2] memory h;
        h[0] = hex"48c9bdf267e6096a3ba7ca8485ae67bb2bf894fe72f36e3cf1361d5f3af54fa5";
        h[1] = hex"d182e6ad7f520e511f6c3e2b8c68059b6bbd41fbabd9831f79217e1319cde05b";

        bytes32[4] memory m;
        m[0] = hex"6162630000000000000000000000000000000000000000000000000000000000";
        m[1] = hex"0000000000000000000000000000000000000000000000000000000000000000";
        m[2] = hex"0000000000000000000000000000000000000000000000000000000000000000";
        m[3] = hex"0000000000000000000000000000000000000000000000000000000000000000";

        bytes8[2] memory t;
        t[0] = hex"03000000";
        t[1] = hex"00000000";

        bool f = true;

        // Expected output:
        // ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d1
        // 7d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923
        return F(rounds, h, m, t, f);
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

    function callCreate2(uint256 salt) public returns(address){
        address addr;
        bytes memory code = type(C).creationCode;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
        }
        return addr;
    }

    function fixCreate2StackDepth(uint salt) public {
        if (isEmptyAddress(callCreate2(salt + 1))) {
            revert();
        }
        this.fixCreate2StackDepth(salt + 1);
    }

    function callCreate() public returns(address){
        address addr;
        bytes memory code = type(C).creationCode;
        assembly {
            addr := create(0, add(code, 0x20), mload(code))
        }
        return addr;
    }

    function fixCreateStackDepth() public {
        if (isEmptyAddress(callCreate())) {
            revert();
        }
        this.fixCreateStackDepth();
    }

    bool constant bool1 = true;
    function isEmptyAddress(address add2) public returns(bool result){

        assembly {
            if iszero(extcodesize(add2)) {
                result := bool1
            }
        }
    }

    function deployef(bytes memory code) public payable{
        address addr;
        assembly {
            addr := create(0, add(code, 0x20), mload(code))
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
    }
}





