pragma solidity ^0.4.11;

contract TRON_ERC721 {
  //name
  function name() constant returns (string name){
    return "Tron ERC721 Token";
  }
  //symbol
  function symbol() constant returns (string symbol){
    return "T721T";
  }

  //totalSupply

  function totalSupply() constant returns (uint256 supply){
    uint256 totalSupply = 1000000000000;
    return totalSupply;
  }

  mapping(address => uint) private balances;
  function balanceOf(address _owner) constant returns (uint balance)
  {
    return balances[_owner];
  }


  mapping(uint256 => address) private tokenOwners;
  mapping(uint256 => bool) private tokenExists;
  function ownerOf(uint256 _tokenId)  constant returns (address owner) {
    require(tokenExists[_tokenId]);
    return tokenOwners[_tokenId];
  }


  mapping(address => mapping (address => uint256)) allowed;
  function approve(address _to, uint256 _tokenId){
    require(msg.sender == ownerOf(_tokenId));
    require(msg.sender != _to);
    allowed[msg.sender][_to] = _tokenId;
    Approval(msg.sender, _to, _tokenId);
  }


  function takeOwnership(uint256 _tokenId){
    require(tokenExists[_tokenId]);
    address oldOwner = ownerOf(_tokenId);
    address newOwner = msg.sender;
    require(newOwner != oldOwner);
    require(allowed[oldOwner][newOwner] == _tokenId);
    balances[oldOwner] -= 1;
    tokenOwners[_tokenId] = newOwner;
    balances[newOwner] += 1;
    Transfer(oldOwner, newOwner, _tokenId);
  }


  mapping(address => mapping(uint256 => uint256)) private ownerTokens;
  function removeFromTokenList(address owner, uint256 _tokenId) private {
    for(uint256 i = 0;ownerTokens[owner][i] != _tokenId;i++){
      ownerTokens[owner][i] = 0;
    }
  }

  function transfer(address _to, uint256 _tokenId){
    address currentOwner = msg.sender;
    address newOwner = _to;
    require(tokenExists[_tokenId]);
    require(currentOwner == ownerOf(_tokenId));
    require(currentOwner != newOwner);
    require(newOwner != address(0));
    address oldOwner =currentOwner;
    removeFromTokenList(oldOwner,_tokenId);
    balances[oldOwner] -= 1;
    tokenOwners[_tokenId] = newOwner;
    balances[newOwner] += 1;
    Transfer(oldOwner, newOwner, _tokenId);
  }

    function transferFrom(address _from,address _to, uint256 _tokenId){
    address currentOwner = _from;
    address newOwner = _to;
    require(tokenExists[_tokenId]);
    require(currentOwner == ownerOf(_tokenId));
    require(currentOwner != newOwner);
    require(newOwner != address(0));
    address oldOwner =currentOwner;
    removeFromTokenList(oldOwner,_tokenId);
    balances[oldOwner] -= 1;
    tokenOwners[_tokenId] = newOwner;
    balances[newOwner] += 1;
    Transfer(oldOwner, newOwner, _tokenId);
  }


  function tokenOfOwnerByIndex(address _owner, uint256 _index) constant returns (uint tokenId){
    return ownerTokens[_owner][_index];
  }


  mapping(uint256 => string) tokenLinks;
  function tokenMetadata(uint256 _tokenId) constant returns (string infoUrl) {
    return tokenLinks[_tokenId];
  }
   // Events
   event Transfer(address indexed _from, address indexed _to, uint256 _tokenId);
   event Approval(address indexed _owner, address indexed _approved, uint256 _tokenId);
}