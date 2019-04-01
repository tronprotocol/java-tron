//pragma solidity ^0.4.0;



contract InnerContract {



    function messageI() payable public returns (uint ret) {



    }

}



contract OuterContract {



    function callInner(address addr) payable public returns (uint) {

        return InnerContract(addr).messageI.value(1)();

    }

}