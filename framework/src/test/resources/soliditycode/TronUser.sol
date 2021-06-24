pragma solidity ^0.4.25;

/**
 * @title Ownable
 * @dev The Ownable contract has an owner address, and provides basic authorization control
 * functions, this simplifies the implementation of "user permissions".
 */
contract Ownable {
    address public owner;


    event OwnershipRenounced(address indexed previousOwner);
    event OwnershipTransferred(
        address indexed previousOwner,
        address indexed newOwner
    );


    /**
     * @dev The Ownable constructor sets the original `owner` of the contract to the sender
     * account.
     */
    constructor() public {
        owner = msg.sender;
    }

    /**
     * @dev Throws if called by any account other than the owner.
     */
    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    /**
     * @dev Allows the current owner to transfer control of the contract to a newOwner.
     * @param _newOwner The address to transfer ownership to.
     */
    function transferOwnership(address _newOwner) public onlyOwner {
        _transferOwnership(_newOwner);
    }

    /**
     * @dev Transfers control of the contract to a newOwner.
     * @param _newOwner The address to transfer ownership to.
     */
    function _transferOwnership(address _newOwner) internal {
        require(_newOwner != address(0));
        emit OwnershipTransferred(owner, _newOwner);
        owner = _newOwner;
    }
}

/**
* @dev A library for working with mutable byte buffers in Solidity.
*
* Byte buffers are mutable and expandable, and provide a variety of primitives
* for writing to them. At any time you can fetch a bytes object containing the
* current contents of the buffer. The bytes object should not be stored between
* operations, as it may change due to resizing of the buffer.
*/
library Buffer {
    /**
    * @dev Represents a mutable buffer. Buffers have a current value (buf) and
    *      a capacity. The capacity may be longer than the current value, in
    *      which case it can be extended without the need to allocate more memory.
    */
    struct buffer {
        bytes buf;
        uint capacity;
    }

    /**
    * @dev Initializes a buffer with an initial capacity.
    * @param buf The buffer to initialize.
    * @param capacity The number of bytes of space to allocate the buffer.
    * @return The buffer, for chaining.
    */
    function init(buffer memory buf, uint capacity) internal pure returns (buffer memory) {
        if (capacity % 32 != 0) {
            capacity += 32 - (capacity % 32);
        }
        // Allocate space for the buffer data
        buf.capacity = capacity;
        assembly {
            let ptr := mload(0x40)
            mstore(buf, ptr)
            mstore(ptr, 0)
            mstore(0x40, add(32, add(ptr, capacity)))
        }
        return buf;
    }

    /**
    * @dev Initializes a new buffer from an existing bytes object.
    *      Changes to the buffer may mutate the original value.
    * @param b The bytes object to initialize the buffer with.
    * @return A new buffer.
    */
    function fromBytes(bytes memory b) internal pure returns (buffer memory) {
        buffer memory buf;
        buf.buf = b;
        buf.capacity = b.length;
        return buf;
    }

    function resize(buffer memory buf, uint capacity) private pure {
        bytes memory oldbuf = buf.buf;
        init(buf, capacity);
        append(buf, oldbuf);
    }

    function max(uint a, uint b) private pure returns (uint) {
        if (a > b) {
            return a;
        }
        return b;
    }

    /**
    * @dev Sets buffer length to 0.
    * @param buf The buffer to truncate.
    * @return The original buffer, for chaining..
    */
    function truncate(buffer memory buf) internal pure returns (buffer memory) {
        assembly {
            let bufptr := mload(buf)
            mstore(bufptr, 0)
        }
        return buf;
    }

    /**
    * @dev Writes a byte string to a buffer. Resizes if doing so would exceed
    *      the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param off The start offset to write to.
    * @param data The data to append.
    * @param len The number of bytes to copy.
    * @return The original buffer, for chaining.
    */
    function write(buffer memory buf, uint off, bytes memory data, uint len) internal pure returns (buffer memory) {
        require(len <= data.length);

        if (off + len > buf.capacity) {
            resize(buf, max(buf.capacity, len + off) * 2);
        }

        uint dest;
        uint src;
        assembly {
        // Memory address of the buffer data
            let bufptr := mload(buf)
        // Length of existing buffer data
            let buflen := mload(bufptr)
        // Start address = buffer address + offset + sizeof(buffer length)
            dest := add(add(bufptr, 32), off)
        // Update buffer length if we're extending it
            if gt(add(len, off), buflen) {
                mstore(bufptr, add(len, off))
            }
            src := add(data, 32)
        }

        // Copy word-length chunks while possible
        for (; len >= 32; len -= 32) {
            assembly {
                mstore(dest, mload(src))
            }
            dest += 32;
            src += 32;
        }

        // Copy remaining bytes
        uint mask = 256 ** (32 - len) - 1;
        assembly {
            let srcpart := and(mload(src), not(mask))
            let destpart := and(mload(dest), mask)
            mstore(dest, or(destpart, srcpart))
        }

        return buf;
    }

    /**
    * @dev Appends a byte string to a buffer. Resizes if doing so would exceed
    *      the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param data The data to append.
    * @param len The number of bytes to copy.
    * @return The original buffer, for chaining.
    */
    function append(buffer memory buf, bytes memory data, uint len) internal pure returns (buffer memory) {
        return write(buf, buf.buf.length, data, len);
    }

    /**
    * @dev Appends a byte string to a buffer. Resizes if doing so would exceed
    *      the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param data The data to append.
    * @return The original buffer, for chaining.
    */
    function append(buffer memory buf, bytes memory data) internal pure returns (buffer memory) {
        return write(buf, buf.buf.length, data, data.length);
    }

    /**
    * @dev Writes a byte to the buffer. Resizes if doing so would exceed the
    *      capacity of the buffer.
    * @param buf The buffer to append to.
    * @param off The offset to write the byte at.
    * @param data The data to append.
    * @return The original buffer, for chaining.
    */
    function writeUint8(buffer memory buf, uint off, uint8 data) internal pure returns (buffer memory) {
        if (off >= buf.capacity) {
            resize(buf, buf.capacity * 2);
        }

        assembly {
        // Memory address of the buffer data
            let bufptr := mload(buf)
        // Length of existing buffer data
            let buflen := mload(bufptr)
        // Address = buffer address + sizeof(buffer length) + off
            let dest := add(add(bufptr, off), 32)
            mstore8(dest, data)
        // Update buffer length if we extended it
            if eq(off, buflen) {
                mstore(bufptr, add(buflen, 1))
            }
        }
        return buf;
    }

    /**
    * @dev Appends a byte to the buffer. Resizes if doing so would exceed the
    *      capacity of the buffer.
    * @param buf The buffer to append to.
    * @param data The data to append.
    * @return The original buffer, for chaining.
    */
    function appendUint8(buffer memory buf, uint8 data) internal pure returns (buffer memory) {
        return writeUint8(buf, buf.buf.length, data);
    }

    /**
    * @dev Writes up to 32 bytes to the buffer. Resizes if doing so would
    *      exceed the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param off The offset to write at.
    * @param data The data to append.
    * @param len The number of bytes to write (left-aligned).
    * @return The original buffer, for chaining.
    */
    function write(buffer memory buf, uint off, bytes32 data, uint len) private pure returns (buffer memory) {
        if (len + off > buf.capacity) {
            resize(buf, (len + off) * 2);
        }

        uint mask = 256 ** len - 1;
        // Right-align data
        data = data >> (8 * (32 - len));
        assembly {
        // Memory address of the buffer data
            let bufptr := mload(buf)
        // Address = buffer address + sizeof(buffer length) + off + len
            let dest := add(add(bufptr, off), len)
            mstore(dest, or(and(mload(dest), not(mask)), data))
        // Update buffer length if we extended it
            if gt(add(off, len), mload(bufptr)) {
                mstore(bufptr, add(off, len))
            }
        }
        return buf;
    }

    /**
    * @dev Writes a bytes20 to the buffer. Resizes if doing so would exceed the
    *      capacity of the buffer.
    * @param buf The buffer to append to.
    * @param off The offset to write at.
    * @param data The data to append.
    * @return The original buffer, for chaining.
    */
    function writeBytes20(buffer memory buf, uint off, bytes20 data) internal pure returns (buffer memory) {
        return write(buf, off, bytes32(data), 20);
    }

    /**
    * @dev Appends a bytes20 to the buffer. Resizes if doing so would exceed
    *      the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param data The data to append.
    * @return The original buffer, for chhaining.
    */
    function appendBytes20(buffer memory buf, bytes20 data) internal pure returns (buffer memory) {
        return write(buf, buf.buf.length, bytes32(data), 20);
    }

    /**
    * @dev Appends a bytes32 to the buffer. Resizes if doing so would exceed
    *      the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param data The data to append.
    * @return The original buffer, for chaining.
    */
    function appendBytes32(buffer memory buf, bytes32 data) internal pure returns (buffer memory) {
        return write(buf, buf.buf.length, data, 32);
    }

    /**
    * @dev Writes an integer to the buffer. Resizes if doing so would exceed
    *      the capacity of the buffer.
    * @param buf The buffer to append to.
    * @param off The offset to write at.
    * @param data The data to append.
    * @param len The number of bytes to write (right-aligned).
    * @return The original buffer, for chaining.
    */
    function writeInt(buffer memory buf, uint off, uint data, uint len) private pure returns (buffer memory) {
        if (len + off > buf.capacity) {
            resize(buf, (len + off) * 2);
        }

        uint mask = 256 ** len - 1;
        assembly {
        // Memory address of the buffer data
            let bufptr := mload(buf)
        // Address = buffer address + off + sizeof(buffer length) + len
            let dest := add(add(bufptr, off), len)
            mstore(dest, or(and(mload(dest), not(mask)), data))
        // Update buffer length if we extended it
            if gt(add(off, len), mload(bufptr)) {
                mstore(bufptr, add(off, len))
            }
        }
        return buf;
    }

    /**
     * @dev Appends a byte to the end of the buffer. Resizes if doing so would
     * exceed the capacity of the buffer.
     * @param buf The buffer to append to.
     * @param data The data to append.
     * @return The original buffer.
     */
    function appendInt(buffer memory buf, uint data, uint len) internal pure returns (buffer memory) {
        return writeInt(buf, buf.buf.length, data, len);
    }
}

library CBOR {

    using Buffer for Buffer.buffer;

    uint8 private constant MAJOR_TYPE_INT = 0;
    uint8 private constant MAJOR_TYPE_NEGATIVE_INT = 1;
    uint8 private constant MAJOR_TYPE_BYTES = 2;
    uint8 private constant MAJOR_TYPE_STRING = 3;
    uint8 private constant MAJOR_TYPE_ARRAY = 4;
    uint8 private constant MAJOR_TYPE_MAP = 5;
    uint8 private constant MAJOR_TYPE_CONTENT_FREE = 7;

    function encodeType(Buffer.buffer memory buf, uint8 major, uint value) private pure {
        if (value <= 23) {
            buf.appendUint8(uint8((major << 5) | value));
        } else if (value <= 0xFF) {
            buf.appendUint8(uint8((major << 5) | 24));
            buf.appendInt(value, 1);
        } else if (value <= 0xFFFF) {
            buf.appendUint8(uint8((major << 5) | 25));
            buf.appendInt(value, 2);
        } else if (value <= 0xFFFFFFFF) {
            buf.appendUint8(uint8((major << 5) | 26));
            buf.appendInt(value, 4);
        } else if (value <= 0xFFFFFFFFFFFFFFFF) {
            buf.appendUint8(uint8((major << 5) | 27));
            buf.appendInt(value, 8);
        }
    }

    function encodeIndefiniteLengthType(Buffer.buffer memory buf, uint8 major) private pure {
        buf.appendUint8(uint8((major << 5) | 31));
    }

    function encodeUInt(Buffer.buffer memory buf, uint value) internal pure {
        encodeType(buf, MAJOR_TYPE_INT, value);
    }

    function encodeInt(Buffer.buffer memory buf, int value) internal pure {
        if (value >= 0) {
            encodeType(buf, MAJOR_TYPE_INT, uint(value));
        } else {
            encodeType(buf, MAJOR_TYPE_NEGATIVE_INT, uint(- 1 - value));
        }
    }

    function encodeBytes(Buffer.buffer memory buf, bytes value) internal pure {
        encodeType(buf, MAJOR_TYPE_BYTES, value.length);
        buf.append(value);
    }

    function encodeString(Buffer.buffer memory buf, string value) internal pure {
        encodeType(buf, MAJOR_TYPE_STRING, bytes(value).length);
        buf.append(bytes(value));
    }

    function startArray(Buffer.buffer memory buf) internal pure {
        encodeIndefiniteLengthType(buf, MAJOR_TYPE_ARRAY);
    }

    function startMap(Buffer.buffer memory buf) internal pure {
        encodeIndefiniteLengthType(buf, MAJOR_TYPE_MAP);
    }

    function endSequence(Buffer.buffer memory buf) internal pure {
        encodeIndefiniteLengthType(buf, MAJOR_TYPE_CONTENT_FREE);
    }
}

/**
 * @title SignedSafeMath
 * @dev Signed math operations with safety checks that revert on error.
 */
library SignedSafeMath {
    int256 constant private _INT256_MIN = - 2 ** 255;

    /**
     * @dev Multiplies two signed integers, reverts on overflow.
     */
    function mul(int256 a, int256 b) internal pure returns (int256) {
        // Gas optimization: this is cheaper than requiring 'a' not being zero, but the
        // benefit is lost if 'b' is also tested.
        // See: https://github.com/OpenZeppelin/openzeppelin-contracts/pull/522
        if (a == 0) {
            return 0;
        }

        require(!(a == - 1 && b == _INT256_MIN), "SignedSafeMath: multiplication overflow");

        int256 c = a * b;
        require(c / a == b, "SignedSafeMath: multiplication overflow");

        return c;
    }

    /**
     * @dev Integer division of two signed integers truncating the quotient, reverts on division by zero.
     */
    function div(int256 a, int256 b) internal pure returns (int256) {
        require(b != 0, "SignedSafeMath: division by zero");
        require(!(b == - 1 && a == _INT256_MIN), "SignedSafeMath: division overflow");

        int256 c = a / b;

        return c;
    }

    /**
     * @dev Subtracts two signed integers, reverts on overflow.
     */
    function sub(int256 a, int256 b) internal pure returns (int256) {
        int256 c = a - b;
        require((b >= 0 && c <= a) || (b < 0 && c > a), "SignedSafeMath: subtraction overflow");

        return c;
    }

    /**
     * @dev Adds two signed integers, reverts on overflow.
     */
    function add(int256 a, int256 b) internal pure returns (int256) {
        int256 c = a + b;
        require((b >= 0 && c >= a) || (b < 0 && c < a), "SignedSafeMath: addition overflow");

        return c;
    }
}


/**
 * @title SafeMath
 * @dev Math operations with safety checks that throw on error
 */
library SafeMath {

    /**
    * @dev Multiplies two numbers, throws on overflow.
    */
    function mul(uint256 _a, uint256 _b) internal pure returns (uint256 c) {
        // Gas optimization: this is cheaper than asserting 'a' not being zero, but the
        // benefit is lost if 'b' is also tested.
        // See: https://github.com/OpenZeppelin/openzeppelin-solidity/pull/522
        if (_a == 0) {
            return 0;
        }

        c = _a * _b;
        assert(c / _a == _b);
        return c;
    }

    /**
    * @dev Integer division of two numbers, truncating the quotient.
    */
    function div(uint256 _a, uint256 _b) internal pure returns (uint256) {
        // assert(_b > 0); // Solidity automatically throws when dividing by 0
        // uint256 c = _a / _b;
        // assert(_a == _b * c + _a % _b); // There is no case in which this doesn't hold
        return _a / _b;
    }

    /**
    * @dev Subtracts two numbers, throws on overflow (i.e. if subtrahend is greater than minuend).
    */
    function sub(uint256 _a, uint256 _b) internal pure returns (uint256) {
        assert(_b <= _a);
        return _a - _b;
    }

    /**
    * @dev Adds two numbers, throws on overflow.
    */
    function add(uint256 _a, uint256 _b) internal pure returns (uint256 c) {
        c = _a + _b;
        assert(c >= _a);
        return c;
    }
}

interface PointerInterface {
    function getAddress() external view returns (address);
}

interface JustlinkRequestInterface {
    function oracleRequest(
        address sender,
        uint256 payment,
        bytes32 id,
        address callbackAddress,
        bytes4 callbackFunctionId,
        uint256 nonce,
        uint256 version,
        bytes data
    ) external;

    function cancelOracleRequest(
        bytes32 requestId,
        uint256 payment,
        bytes4 callbackFunctionId,
        uint256 expiration
    ) external;
}

interface AggregatorInterface {
    function latestAnswer() external view returns (int256);

    function latestTimestamp() external view returns (uint256);

    function latestRound() external view returns (uint256);

    function getAnswer(uint256 roundId) external view returns (int256);

    function getTimestamp(uint256 roundId) external view returns (uint256);

    event AnswerUpdated(int256 indexed current, uint256 indexed roundId, uint256 timestamp);
    event NewRound(uint256 indexed roundId, address indexed startedBy, uint256 startedAt);
}


/**
 * @title Library for common Justlink functions
 * @dev Uses imported CBOR library for encoding to buffer
 */
library Justlink {
    uint256 internal constant defaultBufferSize = 256; // solhint-disable-line const-name-snakecase

    using Buffer for Buffer.buffer;
    using CBOR for Buffer.buffer;

    struct Request {
        bytes32 id;
        address callbackAddress;
        bytes4 callbackFunctionId;
        uint256 nonce;
        Buffer.buffer buf;
    }

    /**
     * @notice Initializes a Justlink request
     * @dev Sets the ID, callback address, and callback function signature on the request
     * @param self The uninitialized request
     * @param _id The Job Specification ID
     * @param _callbackAddress The callback address
     * @param _callbackFunction The callback function signature
     * @return The initialized request
     */
    function initialize(
        Request memory self,
        bytes32 _id,
        address _callbackAddress,
        bytes4 _callbackFunction
    ) internal pure returns (Justlink.Request memory) {
        Buffer.init(self.buf, defaultBufferSize);
        self.id = _id;
        self.callbackAddress = _callbackAddress;
        self.callbackFunctionId = _callbackFunction;
        return self;
    }

    /**
     * @notice Sets the data for the buffer without encoding CBOR on-chain
     * @dev CBOR can be closed with curly-brackets {} or they can be left off
     * @param self The initialized request
     * @param _data The CBOR data
     */
    function setBuffer(Request memory self, bytes _data)
    internal pure
    {
        Buffer.init(self.buf, _data.length);
        Buffer.append(self.buf, _data);
    }

    /**
     * @notice Adds a string value to the request with a given key name
     * @param self The initialized request
     * @param _key The name of the key
     * @param _value The string value to add
     */
    function add(Request memory self, string _key, string _value)
    internal pure
    {
        self.buf.encodeString(_key);
        self.buf.encodeString(_value);
    }

    /**
     * @notice Adds a bytes value to the request with a given key name
     * @param self The initialized request
     * @param _key The name of the key
     * @param _value The bytes value to add
     */
    function addBytes(Request memory self, string _key, bytes _value)
    internal pure
    {
        self.buf.encodeString(_key);
        self.buf.encodeBytes(_value);
    }

    /**
     * @notice Adds a int256 value to the request with a given key name
     * @param self The initialized request
     * @param _key The name of the key
     * @param _value The int256 value to add
     */
    function addInt(Request memory self, string _key, int256 _value)
    internal pure
    {
        self.buf.encodeString(_key);
        self.buf.encodeInt(_value);
    }

    /**
     * @notice Adds a uint256 value to the request with a given key name
     * @param self The initialized request
     * @param _key The name of the key
     * @param _value The uint256 value to add
     */
    function addUint(Request memory self, string _key, uint256 _value)
    internal pure
    {
        self.buf.encodeString(_key);
        self.buf.encodeUInt(_value);
    }

    /**
     * @notice Adds an array of strings to the request with a given key name
     * @param self The initialized request
     * @param _key The name of the key
     * @param _values The array of string values to add
     */
    function addStringArray(Request memory self, string _key, string[] memory _values)
    internal pure
    {
        self.buf.encodeString(_key);
        self.buf.startArray();
        for (uint256 i = 0; i < _values.length; i++) {
            self.buf.encodeString(_values[i]);
        }
        self.buf.endSequence();
    }
}

contract JustMid {

    function setToken(address tokenAddress) public;

    function transferAndCall(address from, address to, uint tokens, bytes _data) public returns (bool success);

    function balanceOf(address guy) public view returns (uint);

    function transferFrom(address src, address dst, uint wad) public returns (bool);

    function allowance(address src, address guy) public view returns (uint);

}

contract TRC20Interface {

    function totalSupply() public view returns (uint);

    function balanceOf(address guy) public view returns (uint);

    function allowance(address src, address guy) public view returns (uint);

    function approve(address guy, uint wad) public returns (bool);

    function transfer(address dst, uint wad) public returns (bool);

    function transferFrom(address src, address dst, uint wad) public returns (bool);

    event Transfer(address indexed from, address indexed to, uint tokens);
    event Approval(address indexed tokenOwner, address indexed spender, uint tokens);
}

/**
 * @title The JustlinkClient contract
 * @notice Contract writers can inherit this contract in order to create requests for the
 * Justlink network
 */
contract JustlinkClient {
    using Justlink for Justlink.Request;
    using SafeMath for uint256;

    uint256 constant internal LINK = 10 ** 18;
    uint256 constant private AMOUNT_OVERRIDE = 0;
    address constant private SENDER_OVERRIDE = 0x0;
    uint256 constant private ARGS_VERSION = 1;

    JustMid internal justMid;
    TRC20Interface internal token;
    JustlinkRequestInterface private oracle;
    uint256 private requests = 1;
    mapping(bytes32 => address) private pendingRequests;

    event JustlinkRequested(bytes32 indexed id);
    event JustlinkFulfilled(bytes32 indexed id);
    event JustlinkCancelled(bytes32 indexed id);

    /**
     * @notice Creates a request that can hold additional parameters
     * @param _specId The Job Specification ID that the request will be created for
     * @param _callbackAddress The callback address that the response will be sent to
     * @param _callbackFunctionSignature The callback function signature to use for the callback address
     * @return A Justlink Request struct in memory
     */
    function buildJustlinkRequest(
        bytes32 _specId,
        address _callbackAddress,
        bytes4 _callbackFunctionSignature
    ) internal pure returns (Justlink.Request memory) {
        Justlink.Request memory req;
        return req.initialize(_specId, _callbackAddress, _callbackFunctionSignature);
    }

    /**
     * @notice Creates a Justlink request to the stored oracle address
     * @dev Calls `JustlinkRequestTo` with the stored oracle address
     * @param _req The initialized Justlink Request
     * @param _payment The amount of LINK to send for the request
     * @return The request ID
     */
    function sendJustlinkRequest(Justlink.Request memory _req, uint256 _payment)
    internal
    returns (bytes32)
    {
        return sendJustlinkRequestTo(oracle, _req, _payment);
    }

    /**
     * @notice Creates a Justlink request to the specified oracle address
     * @dev Generates and stores a request ID, increments the local nonce, and uses `transferAndCall` to
     * send LINK which creates a request on the target oracle contract.
     * Emits JustlinkRequested event.
     * @param _oracle The address of the oracle for the request
     * @param _req The initialized Justlink Request
     * @param _payment The amount of LINK to send for the request
     * @return The request ID
     */
    function sendJustlinkRequestTo(address _oracle, Justlink.Request memory _req, uint256 _payment)
    internal
    returns (bytes32 requestId)
    {
        requestId = keccak256(abi.encodePacked(this, requests));
        _req.nonce = requests;
        pendingRequests[requestId] = _oracle;
        emit JustlinkRequested(requestId);
        token.approve(justMidAddress(), _payment);
        require(justMid.transferAndCall(address(this), _oracle, _payment, encodeRequest(_req)), "unable to transferAndCall to oracle");
        requests += 1;

        return requestId;
    }

    /**
     * @notice Allows a request to be cancelled if it has not been fulfilled
     * @dev Requires keeping track of the expiration value emitted from the oracle contract.
     * Deletes the request from the `pendingRequests` mapping.
     * Emits JustlinkCancelled event.
     * @param _requestId The request ID
     * @param _payment The amount of LINK sent for the request
     * @param _callbackFunc The callback function specified for the request
     * @param _expiration The time of the expiration for the request
     */
    function cancelJustlinkRequest(
        bytes32 _requestId,
        uint256 _payment,
        bytes4 _callbackFunc,
        uint256 _expiration
    )
    internal
    {
        JustlinkRequestInterface requested = JustlinkRequestInterface(pendingRequests[_requestId]);
        delete pendingRequests[_requestId];
        emit JustlinkCancelled(_requestId);
        requested.cancelOracleRequest(_requestId, _payment, _callbackFunc, _expiration);
    }

    /**
     * @notice Sets the stored oracle address
     * @param _oracle The address of the oracle contract
     */
    function setJustlinkOracle(address _oracle) internal {
        oracle = JustlinkRequestInterface(_oracle);
    }

    /**
     * @notice Sets the LINK token address
     * @param _link The address of the LINK token contract
     */
    function setJustlinkToken(address _link) internal {
        token = TRC20Interface(_link);
    }

    function setJustMid(address _justMid) internal {
        justMid = JustMid(_justMid);
    }

    /**
     * @notice Retrieves the stored address of the LINK token
     * @return The address of the LINK token
     */
    function justMidAddress()
    public
    view
    returns (address)
    {
        return address(justMid);
    }

    /**
     * @notice Retrieves the stored address of the oracle contract
     * @return The address of the oracle contract
     */
    function JustlinkOracleAddress()
    internal
    view
    returns (address)
    {
        return address(oracle);
    }

    /**
     * @notice Allows for a request which was created on another contract to be fulfilled
     * on this contract
     * @param _oracle The address of the oracle contract that will fulfill the request
     * @param _requestId The request ID used for the response
     */
    function addJustlinkExternalRequest(address _oracle, bytes32 _requestId)
    internal
    notPendingRequest(_requestId)
    {
        pendingRequests[_requestId] = _oracle;
    }



    /**
     * @notice Encodes the request to be sent to the oracle contract
     * @dev The Justlink node expects values to be in order for the request to be picked up. Order of types
     * will be validated in the oracle contract.
     * @param _req The initialized Justlink Request
     * @return The bytes payload for the `transferAndCall` method
     */
    function encodeRequest(Justlink.Request memory _req)
    private
    view
    returns (bytes memory)
    {
        return abi.encodeWithSelector(
            oracle.oracleRequest.selector,
            SENDER_OVERRIDE, // Sender value - overridden by onTokenTransfer by the requesting contract's address
            AMOUNT_OVERRIDE, // Amount value - overridden by onTokenTransfer by the actual amount of LINK sent
            _req.id,
            _req.callbackAddress,
            _req.callbackFunctionId,
            _req.nonce,
            ARGS_VERSION,
            _req.buf.buf);
    }

    /**
     * @notice Ensures that the fulfillment is valid for this contract
     * @dev Use if the contract developer prefers methods instead of modifiers for validation
     * @param _requestId The request ID for fulfillment
     */
    function validateJustlinkCallback(bytes32 _requestId)
    internal
    recordJustlinkFulfillment(_requestId)
        // solhint-disable-next-line no-empty-blocks
    {}

    /**
     * @dev Reverts if the sender is not the oracle of the request.
     * Emits JustlinkFulfilled event.
     * @param _requestId The request ID for fulfillment
     */
    modifier recordJustlinkFulfillment(bytes32 _requestId) {
        require(msg.sender == pendingRequests[_requestId], "Source must be the oracle of the request");
        delete pendingRequests[_requestId];
        emit JustlinkFulfilled(_requestId);
        _;
    }

    /**
     * @dev Reverts if the request is already pending
     * @param _requestId The request ID for fulfillment
     */
    modifier notPendingRequest(bytes32 _requestId) {
        require(pendingRequests[_requestId] == address(0), "Request is already pending");
        _;
    }
}

/**
 * @title An example Justlink contract with aggregation
 * @notice Requesters can use this contract as a framework for creating
 * requests to multiple Justlink nodes and running aggregation
 * as the contract receives answers.
 */
contract Aggregator is AggregatorInterface, JustlinkClient, Ownable {
    using SignedSafeMath for int256;

    struct Answer {
        uint128 minimumResponses;
        uint128 maxResponses;
        int256[] responses;
    }

    event ResponseReceived(int256 indexed response, uint256 indexed answerId, address indexed sender);

    int256 private currentAnswerValue;
    uint256 private updatedTimestampValue;
    uint256 private latestCompletedAnswer;
    uint128 public paymentAmount;
    uint128 public minimumResponses;
    bytes32[] public jobIds;
    address[] public oracles;

    uint256 private answerCounter = 1;
    mapping(address => bool) public authorizedRequesters;
    mapping(bytes32 => uint256) private requestAnswers;
    mapping(uint256 => Answer) private answers;
    mapping(uint256 => int256) private currentAnswers;
    mapping(uint256 => uint256) private updatedTimestamps;

    uint256 constant private MAX_ORACLE_COUNT = 28;

    /**
     * @notice Deploy with the address of the LINK token and arrays of matching
     * length containing the addresses of the oracles and their corresponding
     * Job IDs.
     * @dev Sets the LinkToken address for the network, addresses of the oracles,
     * and jobIds in storage.
     * @param _link The address of the LINK token
     * @param _justMid The address of the JustMid token
     */
    constructor(address _link, address _justMid) public Ownable() {
        setJustlinkToken(_link);
        setJustMid(_justMid);
        //, uint128 _paymentAmount, uint128 _minimumResponses,
        //        address[] _oracles, bytes32[] _jobIds
        //        updateRequestDetails(_paymentAmount, _minimumResponses, _oracles, _jobIds);
    }

    /**
     * @notice Creates a Justlink request for each oracle in the oracles array.
     * @dev This example does not include request parameters. Reference any documentation
     * associated with the Job IDs used to determine the required parameters per-request.
     */
    function requestRateUpdate()
    payable external
    ensureAuthorizedRequester()
    returns (bytes32)
    {
        require(oracles.length > 0, "Please set oracles and jobIds");
        Justlink.Request memory request;
        bytes32 requestId;
        uint256 oraclePayment = paymentAmount;

        for (uint i = 0; i < oracles.length; i++) {
            request = buildJustlinkRequest(jobIds[i], this, this.justlinkCallback.selector);
            requestId = sendJustlinkRequestTo(oracles[i], request, oraclePayment);
            requestAnswers[requestId] = answerCounter;
        }
        answers[answerCounter].minimumResponses = minimumResponses;
        answers[answerCounter].maxResponses = uint128(oracles.length);

        emit NewRound(answerCounter, msg.sender, block.timestamp);

        answerCounter = answerCounter.add(1);

        return requestId;
    }

    /**
     * @notice Receives the answer from the Justlink node.
     * @dev This function can only be called by the oracle that received the request.
     * @param _clRequestId The Justlink request ID associated with the answer
     * @param _response The answer provided by the Justlink node
     */
    function justlinkCallback(bytes32 _clRequestId, int256 _response)
    external
    {
        validateJustlinkCallback(_clRequestId);

        uint256 answerId = requestAnswers[_clRequestId];
        delete requestAnswers[_clRequestId];

        answers[answerId].responses.push(_response);
        emit ResponseReceived(_response, answerId, msg.sender);
        updateLatestAnswer(answerId);
        deleteAnswer(answerId);
    }


    /**
     * @notice Updates the arrays of oracles and jobIds with new values,
     * overwriting the old values.
     * @dev Arrays are validated to be equal length.
     * @param _paymentAmount the amount of LINK to be sent to each oracle for each request
     * @param _minimumResponses the minimum number of responses
     * before an answer will be calculated
     * @param _oracles An array of oracle addresses
     * @param _jobIds An array of Job IDs
     */
    function updateRequestDetails(
        uint128 _paymentAmount,
        uint128 _minimumResponses,
        address[] _oracles,
        bytes32[] _jobIds
    )
    public
    onlyOwner()
    validateAnswerRequirements(_minimumResponses, _oracles, _jobIds)
    {
        paymentAmount = _paymentAmount;
        minimumResponses = _minimumResponses;
        jobIds = _jobIds;
        oracles = _oracles;
    }

    function getOracleSize() public view returns (uint256)
    {
        return oracles.length;
    }

    /**
     * @notice Allows the owner of the contract to withdraw any LINK balance
     * available on the contract.
     * @dev The contract will need to have a LINK balance in order to create requests.
     * @param _recipient The address to receive the LINK tokens
     * @param _amount The amount of LINK to send from the contract
     */
    function transferLINK(address _recipient, uint256 _amount)
    public
    onlyOwner()
    {
        token.approve(justMidAddress(), _amount);
        require(justMid.transferFrom(address(this), _recipient, _amount), "LINK transfer failed");
    }

    /**
     * @notice Called by the owner to permission other addresses to generate new
     * requests to oracles.
     * @param _requester the address whose permissions are being set
     * @param _allowed boolean that determines whether the requester is
     * permissioned or not
     */
    function setAuthorization(address _requester, bool _allowed)
    external
    onlyOwner()
    {
        authorizedRequesters[_requester] = _allowed;
    }

    /**
     * @notice Cancels an outstanding Justlink request.
     * The oracle contract requires the request ID and additional metadata to
     * validate the cancellation. Only old answers can be cancelled.
     * @param _requestId is the identifier for the Justlink request being cancelled
     * @param _payment is the amount of LINK paid to the oracle for the request
     * @param _expiration is the time when the request expires
     */
    function cancelRequest(
        bytes32 _requestId,
        uint256 _payment,
        uint256 _expiration
    )
    external
    ensureAuthorizedRequester()
    {
        uint256 answerId = requestAnswers[_requestId];
        require(answerId < latestCompletedAnswer, "Cannot modify an in-progress answer");

        delete requestAnswers[_requestId];
        answers[answerId].responses.push(0);
        deleteAnswer(answerId);

        cancelJustlinkRequest(
            _requestId,
            _payment,
            this.justlinkCallback.selector,
            _expiration
        );
    }

    /**
     * @notice Called by the owner to kill the contract. This transfers all LINK
     * balance and ETH balance (if there is any) to the owner.
     */
    function destroy()
    external
    onlyOwner()
    {
        transferLINK(owner, justMid.balanceOf(address(this)));
        selfdestruct(owner);
    }

    /**
     * @dev Performs aggregation of the answers received from the Justlink nodes.
     * Assumes that at least half the oracles are honest and so can't contol the
     * middle of the ordered responses.
     * @param _answerId The answer ID associated with the group of requests
     */
    function updateLatestAnswer(uint256 _answerId)
    private
    ensureMinResponsesReceived(_answerId)
    ensureOnlyLatestAnswer(_answerId)
    {
        uint256 responseLength = answers[_answerId].responses.length;
        uint256 middleIndex = responseLength.div(2);
        int256 currentAnswerTemp;
        if (responseLength % 2 == 0) {
            int256 median1 = quickselect(answers[_answerId].responses, middleIndex);
            int256 median2 = quickselect(answers[_answerId].responses, middleIndex.add(1));
            // quickselect is 1 indexed
            currentAnswerTemp = median1.add(median2) / 2;
            // signed integers are not supported by SafeMath
        } else {
            currentAnswerTemp = quickselect(answers[_answerId].responses, middleIndex.add(1));
            // quickselect is 1 indexed
        }
        currentAnswerValue = currentAnswerTemp;
        latestCompletedAnswer = _answerId;
        updatedTimestampValue = now;
        updatedTimestamps[_answerId] = now;
        currentAnswers[_answerId] = currentAnswerTemp;
        emit AnswerUpdated(currentAnswerTemp, _answerId, now);
    }

    /**
     * @notice get the most recently reported answer
     */
    function latestAnswer()
    external
    view
    returns (int256)
    {
        return currentAnswers[latestCompletedAnswer];
    }

    /**
     * @notice get the last updated at block timestamp
     */
    function latestTimestamp()
    external
    view
    returns (uint256)
    {
        return updatedTimestamps[latestCompletedAnswer];
    }

    /**
     * @notice get past rounds answers
     * @param _roundId the answer number to retrieve the answer for
     */
    function getAnswer(uint256 _roundId)
    external
    view
    returns (int256)
    {
        return currentAnswers[_roundId];
    }

    /**
     * @notice get block timestamp when an answer was last updated
     * @param _roundId the answer number to retrieve the updated timestamp for
     */
    function getTimestamp(uint256 _roundId)
    external
    view
    returns (uint256)
    {
        return updatedTimestamps[_roundId];
    }

    /**
     * @notice get the latest completed round where the answer was updated
     */
    function latestRound()
    external
    view
    returns (uint256)
    {
        return latestCompletedAnswer;
    }

    /**
     * @dev Returns the kth value of the ordered array
     * See: http://www.cs.yale.edu/homes/aspnes/pinewiki/QuickSelect.html
     * @param _a The list of elements to pull from
     * @param _k The index, 1 based, of the elements you want to pull from when ordered
     */
    function quickselect(int256[] memory _a, uint256 _k)
    private
    pure
    returns (int256)
    {
        int256[] memory a = _a;
        uint256 k = _k;
        uint256 aLen = a.length;
        int256[] memory a1 = new int256[](aLen);
        int256[] memory a2 = new int256[](aLen);
        uint256 a1Len;
        uint256 a2Len;
        int256 pivot;
        uint256 i;

        while (true) {
            pivot = a[aLen.div(2)];
            a1Len = 0;
            a2Len = 0;
            for (i = 0; i < aLen; i++) {
                if (a[i] < pivot) {
                    a1[a1Len] = a[i];
                    a1Len++;
                } else if (a[i] > pivot) {
                    a2[a2Len] = a[i];
                    a2Len++;
                }
            }
            if (k <= a1Len) {
                aLen = a1Len;
                (a, a1) = swap(a, a1);
            } else if (k > (aLen.sub(a2Len))) {
                k = k.sub(aLen.sub(a2Len));
                aLen = a2Len;
                (a, a2) = swap(a, a2);
            } else {
                return pivot;
            }
        }
    }

    /**
     * @dev Swaps the pointers to two uint256 arrays in memory
     * @param _a The pointer to the first in memory array
     * @param _b The pointer to the second in memory array
     */
    function swap(int256[] memory _a, int256[] memory _b)
    private
    pure
    returns (int256[] memory, int256[] memory)
    {
        return (_b, _a);
    }

    /**
     * @dev Cleans up the answer record if all responses have been received.
     * @param _answerId The identifier of the answer to be deleted
     */
    function deleteAnswer(uint256 _answerId)
    private
    ensureAllResponsesReceived(_answerId)
    {
        delete answers[_answerId];
    }

    /**
     * @dev Prevents taking an action if the minimum number of responses has not
     * been received for an answer.
     * @param _answerId The the identifier of the answer that keeps track of the responses.
     */
    modifier ensureMinResponsesReceived(uint256 _answerId) {
        if (answers[_answerId].responses.length >= answers[_answerId].minimumResponses) {
            _;
        }
    }

    /**
     * @dev Prevents taking an action if not all responses are received for an answer.
     * @param _answerId The the identifier of the answer that keeps track of the responses.
     */
    modifier ensureAllResponsesReceived(uint256 _answerId) {
        if (answers[_answerId].responses.length == answers[_answerId].maxResponses) {
            _;
        }
    }

    /**
     * @dev Prevents taking an action if a newer answer has been recorded.
     * @param _answerId The current answer's identifier.
     * Answer IDs are in ascending order.
     */
    modifier ensureOnlyLatestAnswer(uint256 _answerId) {
        if (latestCompletedAnswer <= _answerId) {
            _;
        }
    }

    /**
     * @dev Ensures corresponding number of oracles and jobs.
     * @param _oracles The list of oracles.
     * @param _jobIds The list of jobs.
     */
    modifier validateAnswerRequirements(
        uint256 _minimumResponses,
        address[] _oracles,
        bytes32[] _jobIds
    ) {
        require(_oracles.length <= MAX_ORACLE_COUNT, "cannot have more than 45 oracles");
        require(_oracles.length >= _minimumResponses, "must have at least as many oracles as responses");
        require(_oracles.length == _jobIds.length, "must have exactly as many oracles as job IDs");
        _;
    }

    /**
     * @dev Reverts if `msg.sender` is not authorized to make requests.
     */
    modifier ensureAuthorizedRequester() {
        require(authorizedRequesters[msg.sender] || msg.sender == owner, "Not an authorized address for creating requests");
        _;
    }

}
