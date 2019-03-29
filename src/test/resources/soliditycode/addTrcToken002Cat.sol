function unpause(address toAddress, uint256 tokenValue, trcToken tokenId) public onlyCEO whenPaused returns (uint256 r) {
require(saleAuction != address(0));
require(siringAuction != address(0));
require(geneScience != address(0));
require(newContractAddress == address(0));
toAddress.transferToken(tokenValue, tokenId);
r = address(this).tokenBalance(tokenId);
// Actually unpause the contract.
super.unpause();
}