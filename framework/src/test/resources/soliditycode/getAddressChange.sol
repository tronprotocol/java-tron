contract getAddressChange {
    constructor() public payable {}
    // testaddress1函数新增了一个address属性。0.6.0之前 external函数可以通过address(x)来转化为地址，6.0将其禁止，可以通过函数address属性直接获取
    function testaddress1() public view returns(address) {
        //return address(this.getamount); //0.6.0之前可以使用
        return this.getamount.address;  //0.6.0

    }
    function getamount(address) external view returns(uint256) {
        return address(this).balance;
    }
}