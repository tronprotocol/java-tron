contract NotView {
    uint256 public num = 123;
    function setnum() public returns(uint256){
        num = num + 15;
        return num;
    }
}
contract NotViewInterface{
     function setnum() public view returns(uint256);
}
contract UseNotView {
    function setnumuseproxy(address contractAddress) public view returns(uint256){
        NotViewInterface inter = NotViewInterface(contractAddress);
        return inter.setnum();
    }
}