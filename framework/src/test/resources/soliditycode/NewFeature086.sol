contract C {
    constructor() public payable{}

    function catchAssertFail() external view returns(uint) {
        try this.assertFail() {
            return 0;
        } catch Panic(uint _code) {
            if (_code == 0x01) {
                return 0x01;
            }
            return 2;
        }
        return 3;
    }
    function assertFail() external pure {
        assert(0 == 1);
    }
    function catchUnderFlow() external view returns(uint) {
        try this.underflow() {
            return 44;
        } catch Panic(uint _code) {
            if (_code == 0x11) {
                return 0x11;
            }
            return 22;
        }
        return 33;
    }
    function underflow() public pure {
        uint x = 0;
        x--;
    }

    function catchDivideZero() external view returns(uint) {
        try this.divideZero() {
            return 14;
        } catch Panic(uint _code) {
            if (_code == 0x12) {
                return 0x12;
            }
            return 11;
        }
        return 13;
    }
    function divideZero() public pure {
        uint8 x = 0;
        uint8 y = 1;
        uint8 z = y/x;
    }

    function convertUint2Int() public pure {
        uint16 a = 1;
//        int32 b = int32(a);
//        int32 b = a;
    }

    function getAddressCodeLength() public returns(uint) {
        return address(this).code.length;
    }

    function keccak256Bug(string memory s) public returns (bool ret) {
        assembly {
            let a := keccak256(s, 32)
            let b := keccak256(s, 8)
            ret := eq(a, b)
        }
    }

    error InsufficientBalance(uint256 available, uint256 required);
    mapping(address => uint) balance;
    function transfer(address to, uint256 amount) public {
        if (amount > balance[msg.sender])
            revert InsufficientBalance({available: balance[msg.sender], required: amount});
        balance[msg.sender] -= amount;
        balance[to] += amount;
    }

    error Unauthorized();
    function withdraw() public {
        address payable owner;
        if (msg.sender != owner)
            revert Unauthorized();
        owner.transfer(address(this).balance);
    }

    bytes s = "Storage";
    function bytesConcat(bytes calldata c, string memory m, bytes16 b) public view returns(uint256) {
        bytes memory a = bytes.concat(s, c, c[:2], "Literal", bytes(m), b);
        assert((s.length + c.length + 2 + 7 + bytes(m).length + 16) == a.length);
        return a.length;
    }

    bytes p = "hihello";
    function bytesConcatWithEmptyStr() public view {
        bytes memory a = bytes.concat("hi", "", "hello");
        assert(p.length == a.length);
    }

    event ssoo(uint256);
    function testEmitEvent() public payable {
        emit ssoo(6);
    }

    function bytes2BytesN(bytes memory c) public returns (bytes8) {
        // If c is longer than 8 bytes, truncation happens
        return bytes3(c);
    }

    function getContractAddress() public view returns (address a1, address a2) {
        a1 = address(this);
        this.getContractAddress.address;
        [this.getContractAddress.address][0];
        a2 = [this.getContractAddress.address][0];
    }

}





