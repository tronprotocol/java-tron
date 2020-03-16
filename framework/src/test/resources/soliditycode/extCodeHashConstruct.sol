contract CounterConstruct {
     uint count = 0;
     address payable owner;
     event LogResult(bytes32 _hashBefore);
     constructor() public{
         owner = msg.sender;
         address addr = address(this);
         bytes32 _hashBefore;
         assembly {
             _hashBefore := extcodehash(addr)
         }
         emit LogResult(_hashBefore);
     }
 }