//pragma solidity ^0.4.0;



contract InnerContract {

    constructor() public payable{}
    function() external payable{}

    function messageI() payable public returns (uint ret) {



    }

}



contract OuterContract {


    constructor() public payable{}
    function() external payable{}

    function callInner(address payable addr) payable public returns (uint) {

        return InnerContract(addr).messageI.value(1)();

    }

}