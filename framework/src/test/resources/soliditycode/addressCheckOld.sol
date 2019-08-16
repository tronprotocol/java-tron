contract testIsContract{
    function checkAddress(address addr) public returns (address){
        return addr;
    }
    function checkAddress2(address addr) pure public returns (address){
        return addr;
    }
}