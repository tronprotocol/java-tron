

contract arrayLength {
    bytes1[] a;
    uint256[] IntergerArray;
    bytes bs;

    // arrary length
    function arrayPushValue() public returns (bytes1[] memory){
        a = new bytes1[](1);
        a.push(0x01);
        return a;
    }

    function arrayPush() public returns(bytes1[] memory){
        a = new bytes1[](1);
        a.push();
        return a;
    }

    function arrayPop() public returns(bytes1[] memory){
        a = new bytes1[](1);
        a.pop();
        return a;
    }

    // arrary push/pop return Value
    function arrayPushValueReturn() public {
        a = new bytes1[](1);
        return a.push(0x01);
    }

    function arrayPushReturn() public returns (bytes1){
        a = new bytes1[](1);
        return a.push();
    }

    function arrayPopReturn() public{
        a = new bytes1[](1);
        return a.pop();
    }

    function uint256ArrayPushValue() public returns (bytes1[] memory){
        IntergerArray = [1,2,3];
        IntergerArray.push();
        return a;
    }


    // bytes
    function bytesPushValue() public {

        return bs.push(0x01);
    }

    function bytesPush() public returns (bytes1){
        return bs.push();
    }

    function bytesPop() public {
        return bs.pop();
    }

}