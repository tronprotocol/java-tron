contract Test0{
    event FuncCalled(bytes data,uint a);
}

contract Test1 {

    event FuncCalled(string a);
    fallback() external {
        x = "fallback";
        emit FuncCalled(x);
    }
    string x;
}
//含有payable的fallback，无receice
contract Test2 {

    event FuncCalled(string data);
    fallback() external payable{
        x = "fallback";
        emit FuncCalled(x);
    }
    string x;
}

contract TestPayable {
    event FuncCalled(string a);

    fallback() external payable {
        x = "fallback";
        emit FuncCalled(x);
    }

    receive() external payable {
        x = "receive";
        emit FuncCalled(x);
    }
    string x;
}

contract Caller {
    function callTest0(Test0 test) public{
        (bool success,) = address(test).call(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
    }
    function callTest1(address test) public returns (bool) {
        (bool success,) = test.call(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
        (success,) = address(test).call("");
        require(success);
        return true;
    }
    function callTest2(address test) public payable returns (bool) {
        (bool success,) = test.call.value(1000)(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
        return true;
    }
    function callTestPayable1(TestPayable test) public payable returns (bool) {
        (bool success,) = address(test).call(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
        (success,) = address(test).call("");
        require(success);
        return true;
    }
}


//contract Test0 {
//    event FallbackCall(string data,bytes msg);
//    //event FuncCalled(string a,bytes data);
//    function() external payable{
//        x = "fallback";
//        emit FallbackCall(x,msg.data);
//    }
//    string x;
//}
//contract Caller{
//    function call(Test0 test)  public payable returns(bool){
//        (bool success,) = address(test).call(abi.encodeWithSignature("nonExistingFunction()"));
//        require(success);
//        return true;
//    }
//}

