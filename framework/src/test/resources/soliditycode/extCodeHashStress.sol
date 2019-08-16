contract Trigger {
    function test(address addr) public returns(uint i) {
        bytes32 hash;
        while (gasleft() > 1000) {
            assembly {
                hash := extcodehash(addr)
            }
            i++;
        }
    }

    function test(address[] memory addrs) public returns(uint i) {
        bytes32 hash;
        uint i = 0;
        for (; i < addrs.length; i++) {
            address addr = addrs[i];
            assembly {
                hash := extcodehash(addr)
            }
        }
        return i;
    }
 }



 contract TriggerNormal {
    function test(address addr) public returns(uint i) {
       i = 0;
        while (gasleft() > 100000) {
            i++;
        }
    }
 }

  contract TriggerNormal1 {
     function test(address[] memory addrs) public returns(uint i) {
        bytes32 hash;
        uint i = 0;
        for (; i < addrs.length; i++) {
            address addr = addrs[i];
            addr.balance;
        }
     }
  }