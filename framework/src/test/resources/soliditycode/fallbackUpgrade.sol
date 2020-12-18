contract Test0{
    event FuncCalled(bytes data,uint a);
}
//不含payable的fallback,无receive
contract Test1 {
    // 发送到这个合约的所有消息都会调用此函数（因为该合约没有其它函数）。
    // 向这个合约发送币会导致异常，因为 fallback 函数没有 `payable` 修饰符
    event FuncCalled(bytes data,uint a);
    fallback() external {
        x = 12333;
        emit FuncCalled(msg.data, x);
    }
    uint x;
}
//含有payable的fallback，无receice
contract Test2 {
    // 发送到这个合约的所有消息都会调用此函数（因为该合约没有其它函数）。
    // 向这个合约发送币会导致异常，因为 fallback 函数没有 `payable` 修饰符
    event FuncCalled(bytes data,uint a);
    fallback() external payable{
        x = 12333;
        emit FuncCalled(msg.data, x);
    }
    uint x;
}
//含有payable的fallback和receice
// 这个合约会保留所有发送给它的币，没有办法返还。
contract TestPayable {
    event FuncCalled(string a,bytes data);
    // 除了纯转账外，所有的调用都会调用这个函数．
    // (因为除了 receive 函数外，没有其他的函数).
    // 任何对合约非空calldata调用会执行回退函数(即使是调用函数附加币).
    fallback() external payable {
        x = "fallback";
        emit FuncCalled(x,msg.data);
    }
    // 纯转账调用这个函数，例如对每个空empty calldata的调用
    receive() external payable {
        x = "receive";
        emit FuncCalled(x,msg.data);
    }
    string x;
}
// calldata有数据,fallback()函数必须存在且调用fallback()
// calldata无数据,如果有receive()函数就调receive(),如果没有就调用fallback
// 如果两个函数都不存在calldata不论是否有数据均报错
//建议：有多种情况需要测试，是否带转账、calldata是否有值等
contract Caller {
    function callTest0(Test0 test) public{
        //要求必须要有fallback函数，所以返回失败,使用call调用函数，返回的不是函数本身执行的结果
        (bool success,) = address(test).call(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
    }
    function callTest1(Test1 test) public returns (bool) {
        //calldata 有/无数据 都调用fallback
        (bool success,) = address(test).call(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
        (success,) = address(test).call("");
        require(success);
        return true;
    }
    function callTest2(Test2 test) public payable returns(bool) {
        //calldata 有/无数据 都调用fallback
        (bool success,) = address(test).call(abi.encodeWithSignature("nonExistingFunction()"));
        require(success);
        (success,) = address(test).call("");
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


//覆盖solidity版本<0.6.0版本fallback case
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

