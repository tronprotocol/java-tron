
/*
 * 1. caller账户issue一个token
 * 2. caller部署proxy,   传入1000 token，1000 trx
 * 3. caller部署A
 * 4. caller部署B
 * 5. caller调用proxy中upgradetTo函数，传入A的地址
 * 6. caller调用proxy中不存在的trans(uint256,address,trcToken)函数，注意这时trcToken是无意义的，但也带上tokenid。address是任意另外某账户的地址
 * 7. 可以看到目标地址trx增长5，caller账户trx减少5
 * 8. caller调用proxy中upgradeTo函数，传入B的地址
 * 9. caller调用proxy中不存在的trans(uint256,address,trcToken)函数。
 * 10. 可以看到目标地址token增长5，caller账户token减少5
*/
contract Proxy {
  constructor() payable public{}
  address public implementation;
  function upgradeTo(address _address) public {
    implementation = _address;
  }
  fallback() payable external{
    address addr = implementation;
    require(addr != address(0));
    assembly {
      let freememstart := mload(0x40)
      calldatacopy(freememstart, 0, calldatasize())
      let success := delegatecall(not(0), addr, freememstart, calldatasize(), freememstart, 0)
      returndatacopy(freememstart, 0, returndatasize())
      switch success
      case 0 { revert(freememstart, returndatasize()) }
      default { return(freememstart, returndatasize()) }
    }
  }
}

contract A {
    function trans(uint256 amount, address payable toAddress, trcToken id) payable public {
        toAddress.transfer(amount);
    }
}
contract B{
    function trans(uint256 amount, address payable toAddress, trcToken id) payable public {
        toAddress.transferToken(amount,id);
    }
}
