//pragma solidity ^0.4.24;

 contract tokenTest{
      uint256 codesize;
      constructor() payable public{
                   uint256 m;
                   address addr = address(this);
                   assembly {
                       m := extcodesize(addr)
                   }
          codesize = m;
      }

     // positive case
     function pulsone() public payable{
         uint256 j = 0;
         uint i = 100;
         for (; i < i; i++) {
                     j++;
                 }
     }


     function getCodeSize() public returns (uint256){
             return codesize;
      }

 }

 contract confirmTest{

    uint256 codesize;
    constructor() payable public{
        uint256 m;
        address addr = address(this);
        assembly {
            m := extcodesize(addr)

        }
    codesize = m;
    }

    function getCodeSize() public returns (uint256){
        return codesize;
    }

    function confirm(address addr) public returns (uint256){
        uint256 j;
        assembly {
            j := extcodesize(addr)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return j;
    }

    function at(address _addr) public returns (bytes memory o_code) {
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
 }