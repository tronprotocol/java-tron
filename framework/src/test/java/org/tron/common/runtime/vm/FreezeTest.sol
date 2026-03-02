// SPDX-License-Identifier: UNLICENSED
// Reconstructed from FACTORY_CODE bytecode in FreezeTest.java
// Compiler: tron-solc ^0.5.16
//
// FACTORY_CODE contains two nested contracts:
//   Factory (outer) — deploys FreezeContract via CREATE / CREATE2
//   FreezeContract (inner) — freeze/unfreeze operations with TRON-specific opcodes

pragma solidity ^0.5.16;

// ============================================================
// Inner contract — deployed by Factory via CREATE / CREATE2
// ============================================================
contract FreezeContract {

    // selector: 0x00f55d9d
    function destroy(address payable target) external {
        selfdestruct(target);
    }

    // selector: 0x30e1e4e5
    // Freeze TRX for target, then return time remaining until expiry
    function freeze(address payable target, uint256 amount, uint256 res)
        external returns (uint256)
    {
        target.freeze(amount, res);           // TRON opcode 0xd5 (FREEZE)
        // STATICCALL to this.getExpireTime(target, res), then subtract
        return block.timestamp
            - address(this).getExpireTime(target, res);
    }

    // selector: 0x7b46b80b
    function unfreeze(address payable target, uint256 res)
        external returns (uint256)
    {
        target.unfreeze(res);                 // TRON opcode 0xd6 (UNFREEZE)
        return 1;
    }

    // selector: 0xe7aa4e0b
    function getExpireTime(address payable target, uint256 res)
        external view returns (uint256)
    {
        return target.freezeExpireTime(res);  // TRON opcode 0xd7 (FREEZEEXPIRETIME)
    }
}

// ============================================================
// Factory contract — outer layer
// ============================================================
contract Factory {

    // selector: 0x41aa9014
    // Deploy FreezeContract using CREATE (salt is unused, CREATE ignores it)
    function deployCreate2Contract(uint256 salt) public returns (address) {
        bytes memory bytecode = type(FreezeContract).creationCode;
        address addr;
        assembly {
            addr := create(0, add(bytecode, 0x20), mload(bytecode))
        }
        require(extcodesize(addr) > 0);
        return addr;
    }

    // selector: 0xbb63e785
    // Predict CREATE2 address without deploying
    //
    // TRON CREATE2 formula (differs from standard EVM):
    //   address = keccak256(prefix ++ sender[20] ++ salt[32] ++ keccak256(code)[32])[12:]
    //
    // - Standard EVM uses 0xff as prefix (magic byte)
    // - TRON replaces it with the address prefix byte (0x41 for mainnet, 0xa0 for testnet)
    // - This value is hardcoded at compile time by tron-solc
    //
    function getCreate2Addr(uint256 salt) public view returns (address) {
        bytes memory bytecode = type(FreezeContract).creationCode;
        bytes32 hash = keccak256(
            abi.encodePacked(
                bytes1(0x41),       // TRON mainnet address prefix
                address(this),      // 20-byte factory address
                salt,               // 32-byte salt
                keccak256(bytecode) // 32-byte code hash
            )
        );
        return address(uint160(uint256(hash)));
    }
}
