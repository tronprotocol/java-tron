contract callerContract {
    constructor() payable public{}
    fallback() payable external{}
    function sendToB(address called_address,address c) public payable{
       called_address.delegatecall(abi.encodeWithSignature("transferTo(address)",c));
    }
    function sendToB2(address called_address,address c) public payable{
        called_address.call(abi.encodeWithSignature("transferTo(address)",c));
    }
    function sendToB3(address called_address,address c) public payable{
        called_address.delegatecall(abi.encodeWithSignature("transferTo(address)",c));
    }
}

   contract calledContract {
    fallback() payable external {}
    constructor() payable public{}
       function transferTo(address payable toAddress)public payable{
           toAddress.transfer(5);
       }

       function setIinC(address c) public payable{
           c.call.value(5)(abi.encodeWithSignature("setI()"));
       }

   }

   contract c{
       uint256 public i=0;
       constructor() public payable{}
       function getBalance() public view returns(uint256){
           return address(this).balance;
       }
       function setI() payable public{
           i=5;
       }
       fallback() payable external{}
   }
