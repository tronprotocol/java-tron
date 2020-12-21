contract test {
//    uint public x;
//    function setValue1(uint _x) public returns (uint){
//        x = _x;
//        return x;
//    }
    uint public y;
    function setValue3(uint _x) public returns (uint){
        y = _x;
        return y;
    }
}

contract stateVariableShadowing is test {
    uint public x;
    function setValue2(uint _x) public returns (uint){
        x = _x;
        return x;
    }
}

