

 contract tokenTest{
     constructor() public payable{}
     function TransferTokenTo(address payable toAddress, trcToken id,uint256 amount) public payable{
         toAddress.transferToken(amount,id);
     }
 }

contract B{
    uint256 public flag = 0;
    constructor() public payable {}
    fallback()  external {
         flag = 1;
}

}

contract C{
    uint256 public flag = 0;
    constructor() public payable {}
    fallback() external payable {
         //flag = 1;
}

}