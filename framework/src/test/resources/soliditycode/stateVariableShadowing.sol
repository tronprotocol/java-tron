// solidity<0.6.0 A B 各自有自己的 x，调用B.setValue2(100)的结果将是将B.x设置为100，调用B.setValue1(200)的设置将A.x设置为200。
// 0.6.0之后 编译器会报错，不允许同名变量
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

