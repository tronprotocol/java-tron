contract A {
    constructor() public payable{}
    function sssmod() payable public returns (int256) {
        int s1 = 3;
        int s2 = 2;
        int s3 = s1 % s2;
        return s3;
    }

    function eextcodecopy(address _addr) public returns (bytes memory o_code) {
        assembly {
        // retrieve the size of the code, this needs assembly
            let size := extcodesize(_addr)
        // allocate output byte array - this could also be done without assembly
        // by using o_code = new bytes(size)
            o_code := mload(0x40)
        // new "memory end" including padding
            mstore(0x40, add(o_code, and(add(add(size, 0x20), 0x1f), not(0x1f))))
        // store length in memory
            mstore(o_code, size)
        // actually retrieve the code, this needs assembly
            extcodecopy(_addr, add(o_code, 0x20), 0, size)
        }
    }

    function cccoinbase() public returns (address) {
        return block.coinbase;
    }

    function ddifficulty() public returns (uint256) {
        return block.difficulty;
    }

    function gggaslimit() public returns (uint256) {
        return block.gaslimit;
    }

    //pc() is disabled from solidity 0.7.0
//    function ppppc() public returns (uint a) {
//        assembly {
//            a := pc()
//        }
//    }

//    function mmmsize() public returns (uint a) {
//        assembly {
//        // a := pc()
//            a := msize()
//        }
//    }

    function ssswap() public returns (int a) {
        int a=1;
        int b=2;
        int c=3;
        int d=3;
        int e=3;
        int f=3;
        int g=3;
        int h=3;
        int a1=1;
        int b1=2;
        int c1=3;
        int d1=3;
        int e1=3;  //swap 14
        int f1=3;  //swap 15
        int g1=3;  //swap 16
        return (a);
    }

    function pppushx() public returns (uint256) {
            return 0x11223344556677889900112233; //push13
//            return 0x1122334455667788990011223344; //push14
//            return 0x112233445566778899001122334455; //push15
//            return 0x11223344556677889900112233445566; //push16
//            return 0x1122334455667788990011223344556611; //push17
//            return 0x112233445566778899001122334455661111; //push18
//            return 0x11223344556677889900112233445566111111; //push19
//            return 0x112233445566778899001122334455661111111111; //push21
//            return 0x11223344556677889900112233445566111111111111; //push22
//            return 0x1122334455667788990011223344556611111111111111; //push23
//            return 0x112233445566778899001122334455661111111111111111; //push24
//            return 0x11223344556677889900112233445566111111111111111111; //push25
//            return 0x1122334455667788990011223344556611111111111111111111; //push26
//           return 0x112233445566778899001122334455661111111111111111111111; //push27
//           return 0x11223344556677889900112233445566111111111111111111111111; //push28
//         return 0x112233445566778899001122334455661111111111111111111111111111; //push30
    }

    // for test09Callcode
//    function testInCall(address callCAddress,uint256 amount) payable public{
//        callCAddress.call.value(10)(abi.encodeWithSignature("trans(uint256)",amount));
//    }
//    function testIndelegateCall(address callCAddress,uint256 amount) payable public{
//        callCAddress.delegatecall(abi.encodeWithSignature("trans(uint256)",amount));
//    }
//
//    function testInCallcode(address callCAddress,uint256 amount) payable public{
//        callCAddress.callcode.value(10)(abi.encodeWithSignature("trans(uint256)",amount));
//    }
}

//for test09Callcode
//contract C{
//    event clog(address,uint256,uint256);
//    constructor() payable public{}
//    fallback() payable external{}
//    function  trans(uint256 amount) payable public{
//        emit clog(msg.sender,msg.value,amount);
//    }
//
//}