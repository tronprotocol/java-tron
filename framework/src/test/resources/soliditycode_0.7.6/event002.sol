

contract Event {

    event _0();
    event a_0() anonymous;
    event a_4i(uint256 indexed x1, uint256 indexed x2 , uint256 indexed x3, uint256 indexed x4, uint256  x5)anonymous ;
    event _3i(uint256  x1, uint256 indexed x2 , uint256 indexed x3, uint256 x4, uint256  x5) ;
    event _1i(uint256 indexed x1, uint256, uint256 indexed, uint256 x4) ;
    event a_1i(uint256) anonymous;
    event _ai(uint8[2], uint8) ;
    event a_ai(uint8[2], uint8) anonymous;
    event _a1i(uint8[2] indexed, uint8) ;
    event a_a1i(uint8[2] indexed, uint8) anonymous;

    constructor () public {
        // emit a_0();
        // emit a_1i(123);
        // emit a_4i(1,2,3,5,16);
        // emit _0();
        emit _3i(1,2,3,5,16);
        // emit _1i(1,2,3,5);
        // emit _ai([1,2], 3);
        // emit a_ai([3,4], 5);
        // emit _a1i([1,2], 3);
        // emit a_a1i([3,4], 5);
    }

    function e() public {
        emit _1i(1,2,3,4);
    }

    function l() public {
        emit a_1i(1);
    }

    function k() public{
        emit a_4i(2,3,4,5,17);
        emit _3i(2,3,4,5,16);
        emit _1i(2,3,4,5);
        emit a_1i(128);
        emit _0();
        emit a_0();
        //selfdestruct(msg.sender);
        //emit a_4i(1,2,3,5,16);
        //emit _3i(1,2,3,5,16);
        //emit _1i(1,2,3,5);
        //emit a_1i(123);
        //emit _0();
        //emit a_0();
    }
}