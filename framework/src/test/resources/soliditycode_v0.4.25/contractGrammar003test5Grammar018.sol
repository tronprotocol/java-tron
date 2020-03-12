pragma solidity ^0.4.0;


contract Grammar18{
    function testAddmod() public returns (uint z) {
        //计算（x + y）％k，其中以任意精度执行加法，并且不在2 ** 256处围绕
         z=addmod(2, 2, 3);
         return z;
    }
    function testMulmod() public returns (uint z) {
//计算（x * y）％k，其中乘法以任意精度执行，并且不会在2 ** 256处循环。
         z=mulmod(2, 3, 4);
         return z;
    }

  function testKeccak256() public  returns(bytes32){
      //计算的（紧凑）参数的Ethereum-SHA-3（Keccak-256）的散列
      return keccak256("11");
  }

    function testSha256()  public returns(bytes32){
      //计算（紧密包装）参数的SHA-256散列
      return sha256("11");
  }
    function testSha3() public  returns(bytes32){
      //计算（紧密包装）参数的SHA-256散列
      return sha3("11");
  }

    function testRipemd160()  public returns(bytes32){
      //计算（紧密包装）参数的RIPEMD-160哈希值
      return ripemd160("11");
  }


}