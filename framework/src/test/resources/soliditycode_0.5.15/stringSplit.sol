pragma solidity ^0;

contract testStringSplit {
string s1 = "s""1""2"",./";
string s2 = "s123?\\'.";
string s3 = hex"41"hex"42";
string s4 = hex"4142";

function getS1() public view returns (string memory) {
return s1;
}

function getS1N1() public pure returns (string memory) {
string memory n1 = "s""1""2"",./";
return n1;
}

function getS2() public view returns (string memory) {
return s2;
}

function getS2N2() public pure returns (string memory) {
string memory n2 = "s123?\'.";
return n2;
}

function getS3() public view returns (string memory) {
return s3;
}

function getS3N3() public pure returns (string memory) {
string memory n3 = hex"41"hex"42";
return n3;
}

function getS4() public view returns (string memory) {
return s4;
}

function getS4N4() public pure returns (string memory) {
string memory n4 = hex"4142";
return n4;
}

}
