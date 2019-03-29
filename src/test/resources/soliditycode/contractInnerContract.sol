//pragma solidity ^0.4.0;



contract InnerContract {



    function messageI() payable returns (uint ret) {



    }

}



contract OuterContract {



    function callInner(address addr) payable returns (uint) {

        return InnerContract(addr).messageI.value(1)();

    }

}