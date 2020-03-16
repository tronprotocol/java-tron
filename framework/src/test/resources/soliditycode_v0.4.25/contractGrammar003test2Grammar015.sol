pragma solidity ^0.4.0;

contract ExecuteFallback{

  //回退事件，会把调用的数据打印出来
  event FallbackCalled(bytes data);
  //fallback函数，注意是没有名字的，没有参数，没有返回值的
  function(){
    FallbackCalled(msg.data);
  }

  //调用已存在函数的事件，会把调用的原始数据，请求参数打印出来
  event ExistFuncCalled(bytes data, uint256 para);
  //一个存在的函数
  function existFunc(uint256 para){
    ExistFuncCalled(msg.data, para);
  }

  // 模拟从外部对一个存在的函数发起一个调用，将直接调用函数
  function callExistFunc(){
    bytes4 funcIdentifier = bytes4(keccak256("existFunc(uint256)"));
    this.call(funcIdentifier, uint256(1));
  }

  //模拟从外部对一个不存在的函数发起一个调用，由于匹配不到函数，将调用回退函数
  function callNonExistFunc(){
    bytes4 funcIdentifier = bytes4(keccak256("functionNotExist()"));
    this.call(funcIdentifier);
  }

  function ExistFuncCalledTopic() view returns(bytes32){
      return keccak256("ExistFuncCalled(bytes,uint256)");
  }
    function FallbackCalledTopic() view returns(bytes32){
      return keccak256("FallbackCalled(bytes)");
  }
}