contract constantCall {
  bool stopped = false;
  int i = 32482989;
  int i2 = -32482989;
  uint ui = 23487823;
  address origin = 0xdCad3a6d3569DF655070DEd06cb7A1b2Ccd1D3AF;
  bytes32 b32 = 0xb55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105c;
  bytes bs = new bytes(9);
  string s = "123qwe";
  enum ActionChoices { GoLeft, GoRight, GoStraight, SitStill }
  ActionChoices choice = ActionChoices.SitStill;
  int64[] b = [91, 2, 333];
  int32[2][] tmp_h = [[1,2],[3,4],[5,6]];
  int256[2][2] tmp_i = [[11,22],[33,44]];
  mapping (address => uint256) public mapa;

  constructor() payable public{
    mapa[address(0x00)] = 88;
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

  function changeBool(bool param) public constant returns (bool){
    stopped = param;
    log(stopped);
    return stopped;
  }
  function getBool() public constant returns (bool){
    log(stopped);
    return stopped;
  }

  function changeInt(int param) public returns (int){
    i = param;
    log(i);
    return i;
  }
  function getInt() public returns (int){
    log(i);
    return i;
  }

  function changeNegativeInt(int param) public constant returns (int){
    i2 = param;
    log(i2);
    return i2;
  }
  function getNegativeInt() public constant returns (int){
    log(i2);
    return i2;
  }

  function changeUint(uint param) public returns (uint){
    ui = param;
    log(ui);
    return ui;
  }
  function getUint() public returns (uint){
    log(ui);
    return ui;
  }

  function changeAddress(address param) public constant returns (address){
    origin = param;
    log(origin);
    return origin;
  }
  function getAddress() public constant returns (address){
    log(origin);
    return origin;
  }

  function changeBytes32(bytes32 param) public constant returns (bytes32){
    b32 = param;
    log(b32);
    return b32;
  }
  function getBytes32() public returns (bytes32){
    log(b32);
    return b32;
  }

  function changeBytes(bytes param) public constant returns (bytes){
    bs = param;
    log(bs);
    return bs;
  }
  function getBytes() public constant returns (bytes){
    log(bs);
    return bs;
  }

  function changeString(string param) public constant returns (string){
    s = param;
    log(s);
    return s;
  }
  function getString() public returns (string){
    log(s);
    return s;
  }

  function changeActionChoices(ActionChoices param) public constant returns (ActionChoices){
    choice = param;
    log(choice);
    return choice;
  }
  function getActionChoices() public constant returns (ActionChoices){
    log(choice);
    return choice;
  }

  function changeInt64NegativeArray(int64[] param) public constant returns (int64[]){
    b = param;
    log(b);
    return b;
  }
  function getInt64NegativeArray() public constant returns (int64[]){
    log(b);
    return b;
  }

  function changeInt32Array(int32[2][] param) public returns (int32[2][]){
    tmp_h = param;
    log(tmp_h);
    return tmp_h;
  }
  function getInt32Array() public constant returns (int32[2][]){
    log(tmp_h);
    return tmp_h;
  }

  function changeInt256Array(int256[2][2] param) public returns (int256[2][2]){
    tmp_i = param;
    log(tmp_i);
    return tmp_i;
  }
  function getInt256Array() public constant returns (int256[2][2]){
    log(tmp_i);
    return tmp_i;
  }
  function setMapping(uint256 param) public returns (uint256){
        mapa[msg.sender] = param;
        return mapa[msg.sender];

    }
}
