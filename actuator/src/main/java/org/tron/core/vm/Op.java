package org.tron.core.vm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "VM")
public class Op {

  // Halts execution (0x00)
  public static final int STOP = 0x00;

  /*  Arithmetic Operations   */
  // (0x01) Addition operation
  public static final int ADD = 0x01;
  // (0x02) Multiplication operation
  public static final int MUL = 0x02;
  // (0x03) Subtraction operations
  public static final int SUB = 0x03;
  // (0x04) Integer division operation
  public static final int DIV = 0x04;
  // (0x05) Signed integer division operation
  public static final int SDIV = 0x05;
  // (0x06) Modulo remainder operation
  public static final int MOD = 0x06;
  // (0x07) Signed modulo remainder operation
  public static final int SMOD = 0x07;
  // (0x08) Addition combined with modulo remainder operation
  public static final int ADDMOD = 0x08;
  // (0x09) Multiplication combined with modulo remainder operation
  public static final int MULMOD = 0x09;
  // (0x0a) Exponential operation
  public static final int EXP = 0x0a;
  // (0x0b) Extend length of signed integer
  public static final int SIGNEXTEND = 0x0b;

  /*  Bitwise Logic & Comparison Operations   */
  // (0x10) Less-than comparison
  public static final int LT = 0X10;
  // (0x11) Greater-than comparison
  public static final int GT = 0X11;
  // (0x12) Signed less-than comparison
  public static final int SLT = 0X12;
  // (0x13) Signed greater-than comparison
  public static final int SGT = 0X13;
  // (0x14) Equality comparison
  public static final int EQ = 0X14;
  // (0x15) Negation operation
  public static final int ISZERO = 0x15;
  // (0x16) Bitwise AND operation
  public static final int AND = 0x16;
  // (0x17) Bitwise OR operation
  public static final int OR = 0x17;
  // (0x18) Bitwise XOR operation
  public static final int XOR = 0x18;
  // (0x19) Bitwise NOT operation
  public static final int NOT = 0x19;
  // (0x1a) Retrieve single byte from word
  public static final int BYTE = 0x1a;
  // (0x1b) Shift left
  public static final int SHL = 0x1b;
  // (0x1c) Logical shift right
  public static final int SHR = 0x1c;
  // (0x1d) Arithmetic shift right
  public static final int SAR = 0x1d;

  /*  Cryptographic Operations    */
  // (0x20) Compute SHA3-256 hash
  public static final int SHA3 = 0x20;

  /*  Environmental Information   */
  // (0x30)  Get address of currently executing account
  public static final int ADDRESS = 0x30;
  // (0x31) Get balance of the given account
  public static final int BALANCE = 0x31;
  // (0x32) Get execution origination address
  public static final int ORIGIN = 0x32;
  // (0x33) Get caller address
  public static final int CALLER = 0x33;
  // (0x34) Get deposited value by the instruction/transaction responsible for this execution
  public static final int CALLVALUE = 0x34;
  // (0x35) Get input data of current environment
  public static final int CALLDATALOAD = 0x35;
  // (0x36) Get size of input data in current environment
  public static final int CALLDATASIZE = 0x36;
  // (0x37) Copy input data in current environment to memory
  public static final int CALLDATACOPY = 0x37;
  // (0x38) Get size of code running in current environment
  public static final int CODESIZE = 0x38;
  // (0x39) Copy code running in current environment to memory
  public static final int CODECOPY = 0x39;
  public static final int RETURNDATASIZE = 0x3d;
  public static final int RETURNDATACOPY = 0x3e;
  // (0x3a) Get price of gas in current environment
  public static final int GASPRICE = 0x3a;
  // (0x3b) Get size of code running in current environment with given offset
  public static final int EXTCODESIZE = 0x3b;
  // (0x3c) Copy code running in current environment to memory with given offset
  public static final int EXTCODECOPY = 0x3c;
  // (0x3f) Returns the keccak256 hash of a contract’s code
  public static final int EXTCODEHASH = 0x3f;

  /*  Block Information   */
  // (0x40) Get hash of most recent complete block
  public static final int BLOCKHASH = 0x40;
  // (0x41) Get the block’s coinbase address
  public static final int COINBASE = 0x41;
  // (x042) Get the block’s timestamp
  public static final int TIMESTAMP = 0x42;
  // (0x43) Get the block’s number
  public static final int NUMBER = 0x43;
  // (0x44) Get the block’s difficulty
  public static final int DIFFICULTY = 0x44;
  // (0x45) Get the block’s gas limit
  public static final int GASLIMIT = 0x45;
  // (0x46) Get the chain id
  public static final int CHAINID = 0x46;
  // (0x47) Get current account balance
  public static final int SELFBALANCE = 0x47;
  // (0x48) Get block's basefee
  public static final int BASEFEE = 0x48;

  /*  Memory, Storage and Flow Operations */
  // (0x50) Remove item from stack
  public static final int POP = 0x50;
  // (0x51) Load word from memory
  public static final int MLOAD = 0x51;
  // (0x52) Save word to memory
  public static final int MSTORE = 0x52;
  // (0x53) Save byte to memory
  public static final int MSTORE8 = 0x53;
  // (0x54) Load word from storage
  public static final int SLOAD = 0x54;
  // (0x55) Save word to storage
  public static final int SSTORE = 0x55;
  // (0x56) Alter the program counter
  public static final int JUMP = 0x56;
  // (0x57) Conditionally alter the program counter
  public static final int JUMPI = 0x57;
  // (0x58) Get the program counter
  public static final int PC = 0x58;
  // (0x59) Get the size of active memory
  public static final int MSIZE = 0x59;
  // (0x5a) Get the amount of available gas
  public static final int GAS = 0x5a;
  public static final int JUMPDEST = 0x5b;

  /*  Push Operations */
  // Place item on stack
  public static final int PUSH1 = 0x60;
  public static final int PUSH2 = 0x61;
  public static final int PUSH3 = 0x62;
  public static final int PUSH4 = 0x63;
  public static final int PUSH5 = 0x64;
  public static final int PUSH6 = 0x65;
  public static final int PUSH7 = 0x66;
  public static final int PUSH8 = 0x67;
  public static final int PUSH9 = 0x68;
  public static final int PUSH10 = 0x69;
  public static final int PUSH11 = 0x6a;
  public static final int PUSH12 = 0x6b;
  public static final int PUSH13 = 0x6c;
  public static final int PUSH14 = 0x6d;
  public static final int PUSH15 = 0x6e;
  public static final int PUSH16 = 0x6f;
  public static final int PUSH17 = 0x70;
  public static final int PUSH18 = 0x71;
  public static final int PUSH19 = 0x72;
  public static final int PUSH20 = 0x73;
  public static final int PUSH21 = 0x74;
  public static final int PUSH22 = 0x75;
  public static final int PUSH23 = 0x76;
  public static final int PUSH24 = 0x77;
  public static final int PUSH25 = 0x78;
  public static final int PUSH26 = 0x79;
  public static final int PUSH27 = 0x7a;
  public static final int PUSH28 = 0x7b;
  public static final int PUSH29 = 0x7c;
  public static final int PUSH30 = 0x7d;
  public static final int PUSH31 = 0x7e;
  public static final int PUSH32 = 0x7f;

  /*  Duplicate Nth item from the stack   */
  public static final int DUP1 = 0x80;
  public static final int DUP2 = 0x81;
  public static final int DUP3 = 0x82;
  public static final int DUP4 = 0x83;
  public static final int DUP5 = 0x84;
  public static final int DUP6 = 0x85;
  public static final int DUP7 = 0x86;
  public static final int DUP8 = 0x87;
  public static final int DUP9 = 0x88;
  public static final int DUP10 = 0x89;
  public static final int DUP11 = 0x8a;
  public static final int DUP12 = 0x8b;
  public static final int DUP13 = 0x8c;
  public static final int DUP14 = 0x8d;
  public static final int DUP15 = 0x8e;
  public static final int DUP16 = 0x8f;

  /*  Swap the Nth item from the stack with the top   */
  public static final int SWAP1 = 0x90;
  public static final int SWAP2 = 0x91;
  public static final int SWAP3 = 0x92;
  public static final int SWAP4 = 0x93;
  public static final int SWAP5 = 0x94;
  public static final int SWAP6 = 0x95;
  public static final int SWAP7 = 0x96;
  public static final int SWAP8 = 0x97;
  public static final int SWAP9 = 0x98;
  public static final int SWAP10 = 0x99;
  public static final int SWAP11 = 0x9a;
  public static final int SWAP12 = 0x9b;
  public static final int SWAP13 = 0x9c;
  public static final int SWAP14 = 0x9d;
  public static final int SWAP15 = 0x9e;
  public static final int SWAP16 = 0x9f;

  // (0xa[n]) log some data for some addres with 0..n tags [addr [tag0..tagn] data]
  public static final int LOG0 = 0xa0;
  public static final int LOG1 = 0xa1;
  public static final int LOG2 = 0xa2;
  public static final int LOG3 = 0xa3;
  public static final int LOG4 = 0xa4;


  /*  System operations   */
  public static final int CALLTOKEN = 0xd0;
  public static final int TOKENBALANCE = 0xd1;
  public static final int CALLTOKENVALUE = 0xd2;
  public static final int CALLTOKENID = 0xd3;
  public static final int ISCONTRACT = 0xd4;
  public static final int FREEZE = 0xd5;
  public static final int UNFREEZE = 0xd6;
  public static final int FREEZEEXPIRETIME = 0xd7;
  public static final int VOTEWITNESS = 0xd8;
  public static final int WITHDRAWREWARD = 0xd9;
  public static final int FREEZEBALANCEV2 = 0xda;
  public static final int UNFREEZEBALANCEV2 = 0xdb;
  public static final int CANCELALLUNFREEZEV2 = 0xdc;
  public static final int WITHDRAWEXPIREUNFREEZE = 0xdd;
  public static final int DELEGATERESOURCE = 0xde;
  public static final int UNDELEGATERESOURCE = 0xdf;

  // (0xf0) Create a new account with associated code
  public static final int CREATE = 0xf0;
  // Message-call into an account
  public static final int CALL = 0xf1;
  public static final int CALLCODE = 0xf2;
  public static final int DELEGATECALL = 0xf4;
  public static final int STATICCALL = 0xfa;
  // (0xf3) Halt execution returning output data
  public static final int RETURN = 0xf3;
  // (0xf5) Skinny CREATE2, same as CREATE but with deterministic address
  public static final int CREATE2 = 0xf5;
  /*
   * (0xfd) The `REVERT` instruction will stop execution, roll back all state changes done so far
   * and provide a pointer to a memory section, which can be interpreted as an error code or
   * message. While doing so, it will not consume all the remaining gas.
   */
  public static final int REVERT = 0xfd;
  // (0xff) Halt execution and register account for later deletion
  public static final int SUICIDE = 0xff;

  private static final String[] OpName = new String[256];

  private static final Map<String, Byte> stringToByteMap = new HashMap<>();

  static {
    Field[] fields = Op.class.getDeclaredFields();
    for (Field field : fields) {
      try {
        int op;
        if (field.getType() == int.class) {
          op = field.getInt(Op.class);
          OpName[op] = field.getName();
          stringToByteMap.put(field.getName(), (byte) op);
        }
      } catch (IllegalAccessException e) {
        logger.error(e.getMessage());
      }
    }
  }

  public static String getNameOf(int opCode) {
    return OpName[opCode];
  }

  public static String getNameOf(byte opCode) {
    return OpName[opCode & 0xff];
  }

  public static byte getOpOf(String opCode) {
    return stringToByteMap.get(opCode);
  }

}
