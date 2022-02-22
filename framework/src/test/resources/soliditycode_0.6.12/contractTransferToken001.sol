contract A {
    address public a;
     constructor() public payable{}
     function kill(address payable toAddress) payable public{
         selfdestruct(toAddress);
     }
     function newB() public payable returns(address){
         B bAddress=new B();
         a= address(bAddress);
        return a;

     }

 }

contract B{
    constructor() public payable {}
    fallback() external payable {}
    function kill(address payable toAddress) payable public{
         selfdestruct(toAddress);
     }
}