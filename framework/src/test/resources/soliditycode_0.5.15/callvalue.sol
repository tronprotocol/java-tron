contract Callvalue {
function check() public payable returns(uint) {
        uint256 wad;
        assembly {
            wad := callvalue
        }
        return wad;
}
}