pragma solidity ^0.4.25;

// ----------------------------------------------------------------------------
// TRON TRC20
// ----------------------------------------------------------------------------
contract TRC20Interface {

  function totalSupply() public view returns (uint);
  function balanceOf(address guy) public view returns (uint);
  function allowance(address src, address guy) public view returns (uint);
  function approve(address guy, uint wad) public returns (bool);
  function transfer(address dst, uint wad) public returns (bool);
  function transferFrom(address src, address dst, uint wad) public returns (bool);

  event Transfer(address indexed from, address indexed to, uint tokens);
  event Approval(address indexed tokenOwner, address indexed spender, uint tokens);
}

contract Ownable {
  address public owner;


  event OwnershipRenounced(address indexed previousOwner);
  event OwnershipTransferred(
    address indexed previousOwner,
    address indexed newOwner
  );


  /**
   * @dev The Ownable constructor sets the original `owner` of the contract to the sender
   * account.
   */
  constructor() public {
    owner = msg.sender;
  }

  /**
   * @dev Throws if called by any account other than the owner.
   */
  modifier onlyOwner() {
    require(msg.sender == owner);
    _;
  }

  /**
   * @dev Allows the current owner to transfer control of the contract to a newOwner.
   * @param _newOwner The address to transfer ownership to.
   */
  function transferOwnership(address _newOwner) public onlyOwner {
    _transferOwnership(_newOwner);
  }

  /**
   * @dev Transfers control of the contract to a newOwner.
   * @param _newOwner The address to transfer ownership to.
   */
  function _transferOwnership(address _newOwner) internal {
    require(_newOwner != address(0));
    emit OwnershipTransferred(owner, _newOwner);
    owner = _newOwner;
  }
}


contract Receiver {
  function onTokenTransfer(address _sender, uint _value, bytes _data) external;
}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
contract JustMid is Ownable {

  TRC20Interface private token ;

  event Transfer(address indexed from, address indexed to, uint value, bytes data);

  constructor(address _link) public Ownable() {
    token = TRC20Interface(_link);
  }

  function getToken() public view returns(address){
      return address(token);
  }

  function setToken(address tokenAddress) public onlyOwner returns (bool success){
    token = TRC20Interface(tokenAddress);
    return true;
  }

  function transferAndCall(address from, address to, uint tokens, bytes _data) public validRecipient(to) returns (bool success) {
    token.transferFrom(from,to,tokens);
    emit Transfer(from, to, tokens, _data);
    if (isContract(to)) {
      contractFallback(to, tokens, _data);
    }
    return true;
  }

  function transferFrom(address from, address to, uint tokens) public validRecipient(to) returns (bool success) {
    token.transferFrom(from,to,tokens);
    return true;
  }

  function balanceOf(address guy) public view returns (uint) {
      return token.balanceOf(guy);
  }

  function allowance(address src, address guy) public view returns (uint){
      return token.allowance(src, guy);
  }

  modifier validRecipient(address _recipient) {
    require(_recipient != address(0) && _recipient != address(this));
    _;
  }

  function contractFallback(address _to, uint _value, bytes _data) private
  {
    Receiver receiver = Receiver(_to);
    receiver.onTokenTransfer(msg.sender, _value, _data);
  }

   function isContract(address _addr) private returns (bool hasCode)
  {
    uint length;
    assembly { length := extcodesize(_addr) }
    return length > 0;
  }

}
