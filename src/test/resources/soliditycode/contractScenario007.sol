pragma solidity ^0.4.17;

import 'zeppelin-solidity/contracts/token/ERC721/ERC721Token.sol';
import 'zeppelin-solidity/contracts/ownership/Ownable.sol';

/**
 * @title ERC721TokenMock
 * This mock just provides a public mint and burn functions for testing purposes,
 * and a public setter for metadata URI
 */
contract CryptoHerosToken is ERC721Token, Ownable {
  mapping (uint256 => address) internal tokenOwner;
  uint constant minPrice = 0.01 ether;

  string[] public images;
  string[] public backgrounds;
  string[] public descriptions;
  uint[] public numbers;

  struct Hero {
    uint number;
    string image;
    string background;
    string description;
  }

  uint nonce = 0;
  Hero[] public heros;

  mapping(uint256 => Hero) public tokenProperty;

  constructor(string name, string symbol) public
    ERC721Token(name, symbol)
  { }

  function initImage(string _image) public onlyOwner {
    images.push(_image);
  }

  function initBackground(string _background) public onlyOwner {
    backgrounds.push(_background);
  }

  function initNumberAndDescription(uint _number, string _description) public onlyOwner {
    numbers.push(_number);
    descriptions.push(_description);
  }

  /**
   * Only owner can mint
   */
  function mint() public payable {
    require(numbers.length > 0);
    require(images.length > 0);
    require(backgrounds.length > 0);
    require(descriptions.length > 0);
    require(msg.value >= minPrice);
    require(owner.send(msg.value));
    uint256 _tokenId = totalSupply();
    tokenOwner[_tokenId] = msg.sender;
    uint num = rand(0, numbers.length);
    uint _number = numbers[num];
    string memory _image = images[rand(0, images.length)];
    string memory _background = backgrounds[rand(0, backgrounds.length)];
    string memory _description = descriptions[num];
    heros.push(Hero({number: _number, image: _image, background: _background, description: _description}));
    tokenProperty[_tokenId] = Hero({number: _number, image: _image, background: _background, description: _description});
    super._mint(msg.sender, _tokenId);
  }

  function burn(uint256 _tokenId) public onlyOwner {
    tokenOwner[_tokenId] = address(0);
    super._burn(ownerOf(_tokenId), _tokenId);
  }

  function getOwnedTokens(address _owner) external view returns (uint256[]) {
    return ownedTokens[_owner];
  }

  function getTokenProperty(uint256 _tokenId) external view returns (uint _number, string _image, string _background, string _description) {
    return (tokenProperty[_tokenId].number, tokenProperty[_tokenId].image, tokenProperty[_tokenId].background, tokenProperty[_tokenId].description);
  }

  function rand(uint min, uint max) private returns (uint){
    nonce++;
    return uint(sha3(nonce))%(min+max)-min;
  }

  function getHerosLength() external view returns (uint) {
    return heros.length;
  }

  function withdraw(uint amount) public payable onlyOwner returns(bool) {
    require(amount <= this.balance);
    owner.transfer(amount);
    return true;
  }

}