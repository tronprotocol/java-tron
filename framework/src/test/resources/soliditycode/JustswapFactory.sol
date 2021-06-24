pragma solidity ^0.5.8;

import "./JustswapExchange.sol";
import "./IJustswapExchange.sol";


contract JustswapFactory {

    /***********************************|
    |       Events And Variables        |
    |__________________________________*/

    event NewExchange(address indexed token, address indexed exchange);

    address public exchangeTemplate;
    uint256 public tokenCount;
    mapping(address => address) internal token_to_exchange;
    mapping(address => address) internal exchange_to_token;
    mapping(uint256 => address) internal id_to_token;

    /***********************************|
    |         Factory Functions         |
    |__________________________________*/

    function initializeFactory(address template) public {
        require(exchangeTemplate == address(0), "exchangeTemplate already set");
        require(template != address(0), "illegal template");
        exchangeTemplate = template;
    }

    function createExchange(address token) public returns (address) {
        require(token != address(0), "illegal token");
        require(exchangeTemplate != address(0), "exchangeTemplate not set");
        require(token_to_exchange[token] == address(0), "exchange already created");
        JustswapExchange exchange = new JustswapExchange();
        exchange.setup(token);
        token_to_exchange[token] = address(exchange);
        exchange_to_token[address(exchange)] = token;
        uint256 token_id = tokenCount + 1;
        tokenCount = token_id;
        id_to_token[token_id] = token;
        emit NewExchange(token, address(exchange));
        return address(exchange);
    }

    /***********************************|
    |         Getter Functions          |
    |__________________________________*/

    function getExchange(address token) public view returns (address) {
        return token_to_exchange[token];
    }

    function getToken(address exchange) public view returns (address) {
        return exchange_to_token[exchange];
    }

    function getTokenWithId(uint256 token_id) public view returns (address) {
        return id_to_token[token_id];
    }

}

