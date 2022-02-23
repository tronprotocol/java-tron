contract TypeName {
    function testTypeName() public returns (string memory){
        return type(TypeName).name;
    }
}