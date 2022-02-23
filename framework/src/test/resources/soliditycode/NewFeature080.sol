contract C {
    constructor() public payable{}

    function subNoUncheck() public pure returns (uint) {
        uint8 x = 0;
        return x--;
    }

    function subWithUncheck() public pure returns (uint) {
        uint8 x = 0;
        unchecked { x--; }
        return x;
    }

    function addNoUncheck() public pure returns (uint) {
        uint8 x = 255;
        return x++;
    }

    function divideZeroNoUncheck() public pure returns (uint) {
        uint8 x = 0;
        uint8 y = 1;
        return y/x;
    }

    function assertFailNoUncheck() public pure  {
        assert(1==2);
    }

    int64[]   b = [-1, 2, -3];
    function arrayOutofIndex() public view returns (int) {
        return b[3];
    }

    function typeConvert() public pure returns(uint16) {
        // 	 return uint16(int8(-1)); //0.8.0报错 之前不报错
        return uint16(int16(int8(-1)));  //0xffff
        //   return uint16(uint8(int8(-1)));  // 0xff
    }

    function powerMultiRightToLeft() public pure returns(uint) {
        return 2**1**2**3;
    }

    function powerMultiLeftToRight() public pure returns(uint) {
        return ((2**1)**2)**3;
    }

    function powerMultiWith2Params() public pure returns(uint) {
        return 2**3;
    }
    //log 0,1,2,3 disallowed in solidity v0.8.0
//    function f2() public payable {
//        uint256 x=1;
//        uint256 y=2;
//        uint256 z=3;
//        bytes32  _id = bytes32(x);
//        log3(
//            bytes32(x),
//            bytes32(y),
//            bytes32(z),
//            _id
//        );
//    }


    function getBlockChainId111111() view public returns(uint256)  {
        return block.chainid;
    }


    function getBlockChainId() view public returns(uint256) {
        uint256 id;
        assembly {
            id := chainid()
        }
        assert(block.chainid == id);
        return block.chainid;
    }
    function getAddressCodehash(address addr) view public returns(bytes32 newHash)  {
        bytes32 _hashBefore;
        assembly{
            _hashBefore := extcodehash(addr)
        }
        bytes32 newHash = addr.codehash;
        assert(_hashBefore == newHash);
        return newHash;
    }

    function transferToTxorigin(uint64 value) payable public {
        payable(tx.origin).transfer(value);
    }

// msg.data was removed in receive() function
//    event FallbackCalled(bytes data);
//    receive() external payable {
//
//        emit FallbackCalled(msg.data);
//    }

    function transferToLiteralAddress(uint64 value) public{
        uint160 num = type(uint160).max-3;
        address add = address(num);
        payable(add).transfer(value);
    }
}



