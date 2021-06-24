pragma solidity ^0.5.8;

interface IJustswapExchange {
  event TokenPurchase(address indexed buyer, uint256 indexed trx_sold, uint256 indexed tokens_bought);
  event TrxPurchase(address indexed buyer, uint256 indexed tokens_sold, uint256 indexed trx_bought);
  event AddLiquidity(address indexed provider, uint256 indexed trx_amount, uint256 indexed token_amount);
  event RemoveLiquidity(address indexed provider, uint256 indexed trx_amount, uint256 indexed token_amount);

   /**
   * @notice Convert TRX to Tokens.
   * @dev User specifies exact input (msg.value).
   * @dev User cannot specify minimum output or deadline.
   */
  function () external payable;

 /**
   * @dev Pricing function for converting between TRX && Tokens.
   * @param input_amount Amount of TRX or Tokens being sold.
   * @param input_reserve Amount of TRX or Tokens (input type) in exchange reserves.
   * @param output_reserve Amount of TRX or Tokens (output type) in exchange reserves.
   * @return Amount of TRX or Tokens bought.
   */
  function getInputPrice(uint256 input_amount, uint256 input_reserve, uint256 output_reserve) external view returns (uint256);

 /**
   * @dev Pricing function for converting between TRX && Tokens.
   * @param output_amount Amount of TRX or Tokens being bought.
   * @param input_reserve Amount of TRX or Tokens (input type) in exchange reserves.
   * @param output_reserve Amount of TRX or Tokens (output type) in exchange reserves.
   * @return Amount of TRX or Tokens sold.
   */
  function getOutputPrice(uint256 output_amount, uint256 input_reserve, uint256 output_reserve) external view returns (uint256);


  /** 
   * @notice Convert TRX to Tokens.
   * @dev User specifies exact input (msg.value) && minimum output.
   * @param min_tokens Minimum Tokens bought.
   * @param deadline Time after which this transaction can no longer be executed.
   * @return Amount of Tokens bought.
   */ 
  function trxToTokenSwapInput(uint256 min_tokens, uint256 deadline) external payable returns (uint256);

  /** 
   * @notice Convert TRX to Tokens && transfers Tokens to recipient.
   * @dev User specifies exact input (msg.value) && minimum output
   * @param min_tokens Minimum Tokens bought.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output Tokens.
   * @return  Amount of Tokens bought.
   */
  function trxToTokenTransferInput(uint256 min_tokens, uint256 deadline, address recipient) external payable returns(uint256);


  /** 
   * @notice Convert TRX to Tokens.
   * @dev User specifies maximum input (msg.value) && exact output.
   * @param tokens_bought Amount of tokens bought.
   * @param deadline Time after which this transaction can no longer be executed.
   * @return Amount of TRX sold.
   */
  function trxToTokenSwapOutput(uint256 tokens_bought, uint256 deadline) external payable returns(uint256);
  /** 
   * @notice Convert TRX to Tokens && transfers Tokens to recipient.
   * @dev User specifies maximum input (msg.value) && exact output.
   * @param tokens_bought Amount of tokens bought.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output Tokens.
   * @return Amount of TRX sold.
   */
  function trxToTokenTransferOutput(uint256 tokens_bought, uint256 deadline, address recipient) external payable returns (uint256);

  /** 
   * @notice Convert Tokens to TRX.
   * @dev User specifies exact input && minimum output.
   * @param tokens_sold Amount of Tokens sold.
   * @param min_trx Minimum TRX purchased.
   * @param deadline Time after which this transaction can no longer be executed.
   * @return Amount of TRX bought.
   */
  function tokenToTrxSwapInput(uint256 tokens_sold, uint256 min_trx, uint256 deadline) external returns (uint256);

  /** 
   * @notice Convert Tokens to TRX && transfers TRX to recipient.
   * @dev User specifies exact input && minimum output.
   * @param tokens_sold Amount of Tokens sold.
   * @param min_trx Minimum TRX purchased.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output TRX.
   * @return  Amount of TRX bought.
   */
  function tokenToTrxTransferInput(uint256 tokens_sold, uint256 min_trx, uint256 deadline, address recipient) external returns (uint256);

  /** 
   * @notice Convert Tokens to TRX.
   * @dev User specifies maximum input && exact output.
   * @param trx_bought Amount of TRX purchased.
   * @param max_tokens Maximum Tokens sold.
   * @param deadline Time after which this transaction can no longer be executed.
   * @return Amount of Tokens sold.
   */
  function tokenToTrxSwapOutput(uint256 trx_bought, uint256 max_tokens, uint256 deadline) external returns (uint256);

  /**
   * @notice Convert Tokens to TRX && transfers TRX to recipient.
   * @dev User specifies maximum input && exact output.
   * @param trx_bought Amount of TRX purchased.
   * @param max_tokens Maximum Tokens sold.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output TRX.
   * @return Amount of Tokens sold.
   */
  function tokenToTrxTransferOutput(uint256 trx_bought, uint256 max_tokens, uint256 deadline, address recipient) external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (token_addr).
   * @dev User specifies exact input && minimum output.
   * @param tokens_sold Amount of Tokens sold.
   * @param min_tokens_bought Minimum Tokens (token_addr) purchased.
   * @param min_trx_bought Minimum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param token_addr The address of the token being purchased.
   * @return Amount of Tokens (token_addr) bought.
   */
  function tokenToTokenSwapInput(
    uint256 tokens_sold, 
    uint256 min_tokens_bought, 
    uint256 min_trx_bought, 
    uint256 deadline, 
    address token_addr) 
    external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (token_addr) && transfers
   *         Tokens (token_addr) to recipient.
   * @dev User specifies exact input && minimum output.
   * @param tokens_sold Amount of Tokens sold.
   * @param min_tokens_bought Minimum Tokens (token_addr) purchased.
   * @param min_trx_bought Minimum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output TRX.
   * @param token_addr The address of the token being purchased.
   * @return Amount of Tokens (token_addr) bought.
   */
  function tokenToTokenTransferInput(
    uint256 tokens_sold, 
    uint256 min_tokens_bought, 
    uint256 min_trx_bought, 
    uint256 deadline, 
    address recipient, 
    address token_addr) 
    external returns (uint256);


  /**
   * @notice Convert Tokens (token) to Tokens (token_addr).
   * @dev User specifies maximum input && exact output.
   * @param tokens_bought Amount of Tokens (token_addr) bought.
   * @param max_tokens_sold Maximum Tokens (token) sold.
   * @param max_trx_sold Maximum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param token_addr The address of the token being purchased.
   * @return Amount of Tokens (token) sold.
   */
  function tokenToTokenSwapOutput(
    uint256 tokens_bought, 
    uint256 max_tokens_sold, 
    uint256 max_trx_sold, 
    uint256 deadline, 
    address token_addr) 
    external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (token_addr) && transfers
   *         Tokens (token_addr) to recipient.
   * @dev User specifies maximum input && exact output.
   * @param tokens_bought Amount of Tokens (token_addr) bought.
   * @param max_tokens_sold Maximum Tokens (token) sold.
   * @param max_trx_sold Maximum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output TRX.
   * @param token_addr The address of the token being purchased.
   * @return Amount of Tokens (token) sold.
   */
  function tokenToTokenTransferOutput(
    uint256 tokens_bought, 
    uint256 max_tokens_sold, 
    uint256 max_trx_sold, 
    uint256 deadline, 
    address recipient, 
    address token_addr) 
    external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (exchange_addr.token).
   * @dev Allows trades through contracts that were not deployed from the same factory.
   * @dev User specifies exact input && minimum output.
   * @param tokens_sold Amount of Tokens sold.
   * @param min_tokens_bought Minimum Tokens (token_addr) purchased.
   * @param min_trx_bought Minimum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param exchange_addr The address of the exchange for the token being purchased.
   * @return Amount of Tokens (exchange_addr.token) bought.
   */
  function tokenToExchangeSwapInput(
    uint256 tokens_sold, 
    uint256 min_tokens_bought, 
    uint256 min_trx_bought, 
    uint256 deadline, 
    address exchange_addr) 
    external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (exchange_addr.token) && transfers
   *         Tokens (exchange_addr.token) to recipient.
   * @dev Allows trades through contracts that were not deployed from the same factory.
   * @dev User specifies exact input && minimum output.
   * @param tokens_sold Amount of Tokens sold.
   * @param min_tokens_bought Minimum Tokens (token_addr) purchased.
   * @param min_trx_bought Minimum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output TRX.
   * @param exchange_addr The address of the exchange for the token being purchased.
   * @return Amount of Tokens (exchange_addr.token) bought.
   */
  function tokenToExchangeTransferInput(
    uint256 tokens_sold, 
    uint256 min_tokens_bought, 
    uint256 min_trx_bought, 
    uint256 deadline, 
    address recipient, 
    address exchange_addr) 
    external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (exchange_addr.token).
   * @dev Allows trades through contracts that were not deployed from the same factory.
   * @dev User specifies maximum input && exact output.
   * @param tokens_bought Amount of Tokens (token_addr) bought.
   * @param max_tokens_sold Maximum Tokens (token) sold.
   * @param max_trx_sold Maximum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param exchange_addr The address of the exchange for the token being purchased.
   * @return Amount of Tokens (token) sold.
   */
  function tokenToExchangeSwapOutput(
    uint256 tokens_bought, 
    uint256 max_tokens_sold, 
    uint256 max_trx_sold, 
    uint256 deadline, 
    address exchange_addr) 
    external returns (uint256);

  /**
   * @notice Convert Tokens (token) to Tokens (exchange_addr.token) && transfers
   *         Tokens (exchange_addr.token) to recipient.
   * @dev Allows trades through contracts that were not deployed from the same factory.
   * @dev User specifies maximum input && exact output.
   * @param tokens_bought Amount of Tokens (token_addr) bought.
   * @param max_tokens_sold Maximum Tokens (token) sold.
   * @param max_trx_sold Maximum TRX purchased as intermediary.
   * @param deadline Time after which this transaction can no longer be executed.
   * @param recipient The address that receives output TRX.
   * @param exchange_addr The address of the exchange for the token being purchased.
   * @return Amount of Tokens (token) sold.
   */
  function tokenToExchangeTransferOutput(
    uint256 tokens_bought, 
    uint256 max_tokens_sold, 
    uint256 max_trx_sold, 
    uint256 deadline, 
    address recipient, 
    address exchange_addr) 
    external returns (uint256);


  /***********************************|
  |         Getter Functions          |
  |__________________________________*/

  /**
   * @notice external price function for TRX to Token trades with an exact input.
   * @param trx_sold Amount of TRX sold.
   * @return Amount of Tokens that can be bought with input TRX.
   */
  function getTrxToTokenInputPrice(uint256 trx_sold) external view returns (uint256);

  /**
   * @notice external price function for TRX to Token trades with an exact output.
   * @param tokens_bought Amount of Tokens bought.
   * @return Amount of TRX needed to buy output Tokens.
   */
  function getTrxToTokenOutputPrice(uint256 tokens_bought) external view returns (uint256);

  /**
   * @notice external price function for Token to TRX trades with an exact input.
   * @param tokens_sold Amount of Tokens sold.
   * @return Amount of TRX that can be bought with input Tokens.
   */
  function getTokenToTrxInputPrice(uint256 tokens_sold) external view returns (uint256);

  /**
   * @notice external price function for Token to TRX trades with an exact output.
   * @param trx_bought Amount of output TRX.
   * @return Amount of Tokens needed to buy output TRX.
   */
  function getTokenToTrxOutputPrice(uint256 trx_bought) external view returns (uint256);

  /** 
   * @return Address of Token that is sold on this exchange.
   */
  function tokenAddress() external view returns (address);

  /**
   * @return Address of factory that created this exchange.
   */
  function factoryAddress() external view returns (address);


  /***********************************|
  |        Liquidity Functions        |
  |__________________________________*/

  /** 
   * @notice Deposit TRX && Tokens (token) at current ratio to mint UNI tokens.
   * @dev min_liquidity does nothing when total UNI supply is 0.
   * @param min_liquidity Minimum number of UNI sender will mint if total UNI supply is greater than 0.
   * @param max_tokens Maximum number of tokens deposited. Deposits max amount if total UNI supply is 0.
   * @param deadline Time after which this transaction can no longer be executed.
   * @return The amount of UNI minted.
   */
  function addLiquidity(uint256 min_liquidity, uint256 max_tokens, uint256 deadline) external payable returns (uint256);

  /**
   * @dev Burn UNI tokens to withdraw TRX && Tokens at current ratio.
   * @param amount Amount of UNI burned.
   * @param min_trx Minimum TRX withdrawn.
   * @param min_tokens Minimum Tokens withdrawn.
   * @param deadline Time after which this transaction can no longer be executed.
   * @return The amount of TRX && Tokens withdrawn.
   */
  function removeLiquidity(uint256 amount, uint256 min_trx, uint256 min_tokens, uint256 deadline) external returns (uint256, uint256);
}