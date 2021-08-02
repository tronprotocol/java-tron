function a() returns (uint) {
    return 1;
}
abstract contract abvd {

}
interface qwer {
    function getValue() external view returns(uint);
    function getOwner() external view returns(uint);
}
contract C {
    function getOutsideMethod() external returns (uint) {
        return a();
    }
    function getAbstractName() public returns(string memory) {
        return type(abvd).name;
    }
    function getInterfaceName() public returns(string memory) {
        return type(qwer).name;
    }
}
