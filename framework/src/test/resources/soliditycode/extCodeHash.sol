contract TestExtCodeHash {

    function getCodeHashByAddr(address _addr) public returns (bytes32 _hash) {
        assembly {
                _hash := extcodehash(_addr)
            }
    }
    function getCodeHashByUint(uint256 _addr) public returns (bytes32 _hash) {
        assembly {
                _hash := extcodehash(_addr)
            }
    }
}
