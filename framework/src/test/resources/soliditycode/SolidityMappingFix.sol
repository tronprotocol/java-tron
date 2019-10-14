pragma experimental ABIEncoderV2;
contract Tests {
     mapping(address => uint) public balances;
     function update(uint256 amount) public returns (address addr)
     {
         balances[msg.sender] = amount;
         return msg.sender;
     }
}