
contract assemblyTest {

    uint constant x = 1;
    uint constant y = x;
    function getZuint() public view returns (uint) {
        uint z = y + 1;
        assembly {
            z := y
        }
        return z;
    }

    function getZuint2() public returns (uint) {
        uint z = y + 1;
        assembly {
            z := y
        }
        return z;
    }

    bool constant bool1 = true;
    bool constant bool2 = bool1;
    function getZbool() public view returns (bool) {
        bool z;
        assembly {
            z := bool2
        }
        return z;
    }

    function getZbool2() public returns (bool) {
        bool z;
        assembly {
            z := bool2
        }
        return z;
    }


//    string constant string1 = "abc";
//    string constant string2 = string1;
//    function getZstring() public view returns (string memory) {
//        string memory z;
//        assembly {
//            z := string2
//        }
//        return z;
//    }


//    address origin1 = 0xdCad3a6d3569DF655070DEd06cb7A1b2Ccd1D3AF;
//    address origin2 = origin1;
//    function getZaddress() public view returns (address) {
//        address z;
//        assembly {
//            z := origin2
//        }
//        return z;
//    }
}