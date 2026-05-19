pragma solidity ^0.8.25;

contract TvmIssueVerifier {
    function modexpZeroModulus()
        public
        returns (uint256 ok, uint256 sizeAfter, bytes32 copiedWord)
    {
        bytes memory input = new bytes(99);

        assembly {
            let p := add(input, 0x20)
            mstore(p, 1)
            mstore(add(p, 0x20), 1)
            mstore(add(p, 0x40), 1)
            mstore8(add(p, 0x60), 2)
            mstore8(add(p, 0x61), 3)
            mstore8(add(p, 0x62), 0)
            mstore(0, not(0))

            ok := call(gas(), 5, 0, p, 99, 0, 32)
            sizeAfter := returndatasize()
            copiedWord := mload(0)
        }
    }

    function failedCreate2KeepsPriorReturnData(bytes memory code, uint256 salt)
        public
        returns (uint256 beforeSize, address created, uint256 afterSize)
    {
        assembly {
            mstore(0, 0x1234)
            pop(call(gas(), 4, 0, 0, 32, 0, 32))
            beforeSize := returndatasize()

            created := create2(1, add(code, 0x20), mload(code), salt)
            afterSize := returndatasize()
        }
    }

    function successfulCreate2KeepsPriorReturnData(bytes memory code, uint256 salt)
        public
        returns (uint256 beforeSize, address created, uint256 afterSize, uint256 createdSize)
    {
        assembly {
            mstore(0, 0x1234)
            pop(call(gas(), 4, 0, 0, 32, 0, 32))
            beforeSize := returndatasize()

            created := create2(0, add(code, 0x20), mload(code), salt)
            afterSize := returndatasize()
            createdSize := extcodesize(created)
        }
    }

}
