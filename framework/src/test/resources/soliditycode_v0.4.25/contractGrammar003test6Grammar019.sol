pragma solidity ^0.4.0;
contract timetest {

function timetest() public {
require( 1 == 1 seconds);
require(1 minutes == 60 seconds);
require(1 hours == 60 minutes);
require(1 days == 24 hours);
require(1 weeks == 7 days);
require(1 years == 365 days);
}
}