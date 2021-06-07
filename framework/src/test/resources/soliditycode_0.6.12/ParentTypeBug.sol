contract Parent {
    uint256 public m_aMember;
    address public m_bMember;
}
contract Child is Parent {
    function foo() public view returns (uint256) { return Parent.m_aMember; }
    function bar() public view returns (address) { return Parent.m_bMember; }

    // complie failed
    //    function foo() public pure returns (uint256) { return Parent.m_aMember; }
    //    function bar() public pure returns (address) { return Parent.m_bMember; }

}
