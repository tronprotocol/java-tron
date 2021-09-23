contract NotView {
    uint256 public num = 123;
    function setnum() public returns(uint256){
        num = num + 15;
        return num;
    }
}
contract NotViewInterface{
     function setnum() public returns(uint256);
}
contract UseNotView {
    function setnumuseproxy(address contractAddress) public returns(uint256){
        NotViewInterface inter = NotViewInterface(contractAddress);
        return inter.setnum();
    }
}
contract viewCall {
  bool stopped = false;
  int i = 32482989;
  int i2 = -32482989;
  uint ui = 23487823;
  address origin = 0xdCad3a6d3569DF655070DEd06cb7A1b2Ccd1D3AF;
  bytes32 b32 = bytes32(uint256(0xdCad3a6d3569DF655070DEd0));
  bytes bs = new bytes(3);
  string s = "123qwe";
  enum ActionChoices { GoLeft, GoRight, GoStraight, SitStill }
  ActionChoices choice = ActionChoices.GoRight;
  int64[] b = [-1, 2, -3];
  int32[2][] tmp_h = [[1,2],[3,4],[5,6]];
  int256[2][2] tmp_i = [[11,22],[33,44]];
  mapping (address => uint256) public mapa;
  constructor() payable public{
     mapa[address(0x00)] = 34;
  }
  event log(int);
  event log(uint);
  event log(bool);
  event log(address);
  event log(bytes32);
  event log(bytes);
  event log(string);
  event log(ActionChoices);
  event log(int64[]);
  event log(int32[2][]);
  event log(int256[2][2]);
  function changeBool(bool param) public returns (bool){
    stopped = param;
    emit log(stopped);
    return stopped;
  }
  function getBool() public returns (bool){
    emit log(stopped);
    return stopped;
  }
  function changeInt(int param) public returns (int){
    i = param;
    emit log(i);
    return i;
  }
  function getInt() public returns (int){
    emit log(i);
    return i;
  }
  function changeNegativeInt(int param) public returns (int){
    i2 = param;
    emit log(i2);
    return i2;
  }
  function getNegativeInt() public returns (int){
    emit log(i2);
    return i2;
  }
  function changeUint(uint param) public returns (uint){
    ui = param;
    emit log(ui);
    return ui;
  }
  function getUint() public returns (uint){
    emit log(ui);
    return ui;
  }
  function changeAddress(address param) public returns (address){
    origin = param;
    emit log(origin);
    return origin;
  }
  function getAddress() public returns (address){
    emit log(origin);
    return origin;
  }
  function changeBytes32(bytes32 param) public returns (bytes32){
    b32 = param;
    emit log(b32);
    return b32;
  }
  function getBytes32() public returns (bytes32){
    emit log(b32);
    return b32;
  }
  function changeBytes(bytes memory param) public returns (bytes memory){
    bs = param;
    emit log(bs);
    return bs;
  }
  function getBytes() public returns (bytes memory){
    emit log(bs);
    return bs;
  }
  function changeString(string memory param) public returns (string memory){
    s = param;
    emit log(s);
    return s;
  }
  function getString() public returns (string memory){
    emit log(s);
    return s;
  }
  function changeActionChoices(ActionChoices param) public returns (ActionChoices){
    choice = param;
    emit log(choice);
    return choice;
  }
  function getActionChoices() public returns (ActionChoices){
    emit log(choice);
    return choice;
  }
  function changeInt64NegativeArray(int64[] memory param) public returns (int64[] memory){
    b = param;
    emit log(b);
    return b;
  }
  function getInt64NegativeArray() public returns (int64[] memory){
    emit log(b);
    return b;
  }
  function changeInt32Array(int32[2][] memory param) public returns (int32[2][] memory){
    tmp_h = param;
    emit log(tmp_h);
    return tmp_h;
  }
  function getInt32Array() public returns (int32[2][] memory){
    emit log(tmp_h);
    return tmp_h;
  }
  function changeInt256Array(int256[2][2] memory param) public returns (int256[2][2] memory){
    tmp_i = param;
    emit log(tmp_i);
    return tmp_i;
  }
  function getInt256Array() public returns (int256[2][2] memory){
    emit log(tmp_i);
    return tmp_i;
  }
  function setMapping(uint256 param) public returns (uint256){
      mapa[msg.sender] = param;
      return mapa[msg.sender];

  }
}