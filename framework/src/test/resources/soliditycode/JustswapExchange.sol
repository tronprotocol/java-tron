pragma solidity ^0.5.8;

import "./TRC20.sol";
import "./ITRC20.sol";
import "./IJustswapFactory.sol";
import "./IJustswapExchange.sol";
import "./ReentrancyGuard.sol";
import "./TransferHelper.sol";


contract JustswapExchange is TRC20, ReentrancyGuard {

    /***********************************|
    |        Variables && Events        |
    |__________________________________*/

    // Variables
    string public name;         // Justswap V1
    string public symbol;       // JUSTSWAP-V1
    uint256 public decimals;     // 6
    ITRC20 token;                // address of the TRC20 token traded on this contract
    IJustswapFactory factory;     // interface for the factory that created this contract
    using TransferHelper for address;

    // Events
    event TokenPurchase(address indexed buyer, uint256 indexed trx_sold, uint256 indexed tokens_bought);
    event TrxPurchase(address indexed buyer, uint256 indexed tokens_sold, uint256 indexed trx_bought);
    event AddLiquidity(address indexed provider, uint256 indexed trx_amount, uint256 indexed token_amount);
    event RemoveLiquidity(address indexed provider, uint256 indexed trx_amount, uint256 indexed token_amount);
    event Snapshot(address indexed operator, uint256 indexed trx_balance, uint256 indexed token_balance);


    /***********************************|
    |            Constsructor           |
    |__________________________________*/

    /**
     * @dev This function acts as a contract constructor which is not currently supported in contracts deployed
     *      using create_with_code_of(). It is called once by the factory during contract creation.
     */
    function setup(address token_addr) public {
        require(
            address(factory) == address(0) && address(token) == address(0) && token_addr != address(0),
            "INVALID_ADDRESS"
        );
        factory = IJustswapFactory(msg.sender);
        token = ITRC20(token_addr);
        name = "Justswap V1";
        symbol = "JUSTSWAP-V1";
        decimals = 6;
    }


    /***********************************|
    |        Exchange Functions         |
    |__________________________________*/


    /**
     * @notice Convert TRX to Tokens.
     * @dev User specifies exact input (msg.value).
     * @dev User cannot specify minimum output or deadline.
     */
    function() external payable {
        trxToTokenInput(msg.value, 1, block.timestamp, msg.sender, msg.sender);
    }

    /**
      * @dev Pricing function for converting between TRX && Tokens.
      * @param input_amount Amount of TRX or Tokens being sold.
      * @param input_reserve Amount of TRX or Tokens (input type) in exchange reserves.
      * @param output_reserve Amount of TRX or Tokens (output type) in exchange reserves.
      * @return Amount of TRX or Tokens bought.
      */

    // 去除手续费amount=input_amount*997=input_amount_with_fee
    // new_output_reserve=output_reserve-output_amount
    // new_input_reserve=input_reserve+amount
    // new_output_reserve*new_input_reserve=output_reserve*input_reserve=不变的K值
    // new_output_reserve*new_input_reserve=(output_reserve-output_amount)*(input_reserve+amount)
    // x*y=(x-a)*(y+b)
    // => x*y=x*y+x*b-a*y-a*b => a*y+a*b=x*b => a*(y+b)=x*b
    // => a=x*b/(y+b)
    // output_amount = output_reserve*input_amount_with_fee/(input_reserve+input_amount_with_fee)
    function getInputPrice(uint256 input_amount, uint256 input_reserve, uint256 output_reserve) public view returns (uint256) {
        require(input_reserve > 0 && output_reserve > 0, "INVALID_VALUE");
        uint256 input_amount_with_fee = input_amount.mul(997);
        uint256 numerator = input_amount_with_fee.mul(output_reserve);
        uint256 denominator = input_reserve.mul(1000).add(input_amount_with_fee);
        return numerator.div(denominator);

    }

    /**
      * @dev Pricing function for converting between TRX && Tokens.
      * @param output_amount Amount of TRX or Tokens being bought.
      * @param input_reserve Amount of TRX or Tokens (input type) in exchange reserves.
      * @param output_reserve Amount of TRX or Tokens (output type) in exchange reserves.
      * @return Amount of TRX or Tokens sold.
      */
    // new_output_reserve=output_reserve-output_amount
    // new_input_reserve=input_reserve+input_amount
    // new_output_reserve*new_input_reserve=output_reserve*input_reserve=不变的K值
    // new_output_reserve*new_input_reserve=(output_reserve-output_amount)*(input_reserve+input_amount)
    // x*y=(x-a)*(y+b)
    // => x*y=x*y+x*b-a*y-a*b => a*y=x*b-a*b => a*y=(x-a)*b
    // => b=y*a/(x-a)
    // input_amount = input_reserve*output_amount/(output_reserve-output_amount)
    // real_intput_amount=input_amount/0.997+1
    function getOutputPrice(uint256 output_amount, uint256 input_reserve, uint256 output_reserve) public view returns (uint256) {
        require(input_reserve > 0 && output_reserve > 0);
        uint256 numerator = input_reserve.mul(output_amount).mul(1000);
        uint256 denominator = (output_reserve.sub(output_amount)).mul(997);
        return (numerator.div(denominator)).add(1);
    }

    function trxToTokenInput(uint256 trx_sold, uint256 min_tokens, uint256 deadline, address buyer, address recipient) private nonReentrant returns (uint256) {
        require(deadline >= block.timestamp && trx_sold > 0 && min_tokens > 0, "trxToTokenInput-require");
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 tokens_bought = getInputPrice(trx_sold, address(this).balance.sub(trx_sold), token_reserve);
        require(tokens_bought >= min_tokens, "tokens_bought<min_tokens");

        require(address(token).safeTransfer(address(recipient), tokens_bought), "safeTransfer failed");
        emit TokenPurchase(buyer, trx_sold, tokens_bought);
        emit Snapshot(buyer, address(this).balance, token.balanceOf(address(this)));
        return tokens_bought;
    }

    /**
     * @notice Convert TRX to Tokens.
     * @dev User specifies exact input (msg.value) && minimum output.
     * @param min_tokens Minimum Tokens bought.
     * @param deadline Time after which this transaction can no longer be executed.
     * @return Amount of Tokens bought.
     */
    function trxToTokenSwapInput(uint256 min_tokens, uint256 deadline) public payable returns (uint256)  {
        return trxToTokenInput(msg.value, min_tokens, deadline, msg.sender, msg.sender);
    }

    /**
     * @notice Convert TRX to Tokens && transfers Tokens to recipient.
     * @dev User specifies exact input (msg.value) && minimum output
     * @param min_tokens Minimum Tokens bought.
     * @param deadline Time after which this transaction can no longer be executed.
     * @param recipient The address that receives output Tokens.
     * @return  Amount of Tokens bought.
     */
    function trxToTokenTransferInput(uint256 min_tokens, uint256 deadline, address recipient) public payable returns (uint256) {
        require(recipient != address(this) && recipient != address(0));
        return trxToTokenInput(msg.value, min_tokens, deadline, msg.sender, recipient);
    }

    function trxToTokenOutput(uint256 tokens_bought, uint256 max_trx, uint256 deadline, address payable buyer, address recipient) private nonReentrant returns (uint256) {
        require(deadline >= block.timestamp && tokens_bought > 0 && max_trx > 0);
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 trx_sold = getOutputPrice(tokens_bought, address(this).balance.sub(max_trx), token_reserve);
        // Throws if trx_sold > max_trx
        uint256 trx_refund = max_trx.sub(trx_sold);
        if (trx_refund > 0) {
            buyer.transfer(trx_refund);
        }

        require(address(token).safeTransfer(recipient, tokens_bought));
        emit TokenPurchase(buyer, trx_sold, tokens_bought);
        emit Snapshot(buyer, address(this).balance, token.balanceOf(address(this)));
        return trx_sold;
    }

    /**
     * @notice Convert TRX to Tokens.
     * @dev User specifies maximum input (msg.value) && exact output.
     * @param tokens_bought Amount of tokens bought.
     * @param deadline Time after which this transaction can no longer be executed.
     * @return Amount of TRX sold.
     */
    function trxToTokenSwapOutput(uint256 tokens_bought, uint256 deadline) public payable returns (uint256) {
        return trxToTokenOutput(tokens_bought, msg.value, deadline, msg.sender, msg.sender);
    }

    /**
     * @notice Convert TRX to Tokens && transfers Tokens to recipient.
     * @dev User specifies maximum input (msg.value) && exact output.
     * @param tokens_bought Amount of tokens bought.
     * @param deadline Time after which this transaction can no longer be executed.
     * @param recipient The address that receives output Tokens.
     * @return Amount of TRX sold.
     */
    function trxToTokenTransferOutput(uint256 tokens_bought, uint256 deadline, address recipient) public payable returns (uint256) {
        require(recipient != address(this) && recipient != address(0));
        //通过msg.value控制滑点 , 多余的trx返还
        return trxToTokenOutput(tokens_bought, msg.value, deadline, msg.sender, recipient);
    }

    // 997 * tokens_sold / 1000 =  本金
    // 3 * tokens_sold / 1000 = fee
    // tokens_reserve +  (997 * tokens_sold / 1000) = trx_reserve - x
    // x = ?
    // token_amount = token_reserve*trx_amount/(trx_reserve-trx_amount)
    // real_token_amount=toekn_amount/0.997+1

    function tokenToTrxInput(uint256 tokens_sold, uint256 min_trx, uint256 deadline, address buyer, address payable recipient) private nonReentrant returns (uint256) {
        require(deadline >= block.timestamp && tokens_sold > 0 && min_trx > 0);
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 trx_bought = getInputPrice(tokens_sold, token_reserve, address(this).balance);
        uint256 wei_bought = trx_bought;
        require(wei_bought >= min_trx);
        recipient.transfer(wei_bought);

        require(address(token).safeTransferFrom(buyer, address(this), tokens_sold));
        emit TrxPurchase(buyer, tokens_sold, wei_bought);
        emit Snapshot(buyer, address(this).balance, token.balanceOf(address(this)));

        return wei_bought;
    }

    /**
     * @notice Convert Tokens to TRX.
     * @dev User specifies exact input && minimum output.
     * @param tokens_sold Amount of Tokens sold.
     * @param min_trx Minimum TRX purchased.
     * @param deadline Time after which this transaction can no longer be executed.
     * @return Amount of TRX bought.
     */
    function tokenToTrxSwapInput(uint256 tokens_sold, uint256 min_trx, uint256 deadline) public returns (uint256) {
        return tokenToTrxInput(tokens_sold, min_trx, deadline, msg.sender, msg.sender);
    }

    /**
     * @notice Convert Tokens to TRX && transfers TRX to recipient.
     * @dev User specifies exact input && minimum output.
     * @param tokens_sold Amount of Tokens sold.
     * @param min_trx Minimum TRX purchased.
     * @param deadline Time after which this transaction can no longer be executed.
     * @param recipient The address that receives output TRX.
     * @return  Amount of TRX bought.
     */
    function tokenToTrxTransferInput(uint256 tokens_sold, uint256 min_trx, uint256 deadline, address payable recipient) public returns (uint256) {
        require(recipient != address(this) && recipient != address(0));
        return tokenToTrxInput(tokens_sold, min_trx, deadline, msg.sender, recipient);
    }


    function tokenToTrxOutput(uint256 trx_bought, uint256 max_tokens, uint256 deadline, address buyer, address payable recipient) private nonReentrant returns (uint256) {
        require(deadline >= block.timestamp && trx_bought > 0);
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 tokens_sold = getOutputPrice(trx_bought, token_reserve, address(this).balance);
        // tokens sold is always > 0
        require(max_tokens >= tokens_sold);
        recipient.transfer(trx_bought);

        require(address(token).safeTransferFrom(buyer, address(this), tokens_sold));
        emit TrxPurchase(buyer, tokens_sold, trx_bought);
        emit Snapshot(buyer, address(this).balance, token.balanceOf(address(this)));
        return tokens_sold;
    }

    /**
     * @notice Convert Tokens to TRX.
     * @dev User specifies maximum input && exact output.
     * @param trx_bought Amount of TRX purchased.
     * @param max_tokens Maximum Tokens sold.
     * @param deadline Time after which this transaction can no longer be executed.
     * @return Amount of Tokens sold.
     */
    function tokenToTrxSwapOutput(uint256 trx_bought, uint256 max_tokens, uint256 deadline) public returns (uint256) {
        return tokenToTrxOutput(trx_bought, max_tokens, deadline, msg.sender, msg.sender);
    }

    /**
     * @notice Convert Tokens to TRX && transfers TRX to recipient.
     * @dev User specifies maximum input && exact output.
     * @param trx_bought Amount of TRX purchased.
     * @param max_tokens Maximum Tokens sold.
     * @param deadline Time after which this transaction can no longer be executed.
     * @param recipient The address that receives output TRX.
     * @return Amount of Tokens sold.
     */
    function tokenToTrxTransferOutput(uint256 trx_bought, uint256 max_tokens, uint256 deadline, address payable recipient) public returns (uint256) {
        require(recipient != address(this) && recipient != address(0));
        return tokenToTrxOutput(trx_bought, max_tokens, deadline, msg.sender, recipient);
    }

    function tokenToTokenInput(
        uint256 tokens_sold,
        uint256 min_tokens_bought,
        uint256 min_trx_bought,
        uint256 deadline,
        address buyer,
        address recipient,
        address payable exchange_addr)
    nonReentrant
    private returns (uint256)
    {
        require(deadline >= block.timestamp && tokens_sold > 0 && min_tokens_bought > 0 && min_trx_bought > 0, "illegal input parameters");
        require(exchange_addr != address(this) && exchange_addr != address(0), "illegal exchange addr");
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 trx_bought = getInputPrice(tokens_sold, token_reserve, address(this).balance);
        uint256 wei_bought = trx_bought;
        require(wei_bought >= min_trx_bought, "min trx bought not matched");

        require(address(token).safeTransferFrom(buyer, address(this), tokens_sold), "transfer failed");
        uint256 tokens_bought = IJustswapExchange(exchange_addr).trxToTokenTransferInput.value(wei_bought)(min_tokens_bought, deadline, recipient);
        emit TrxPurchase(buyer, tokens_sold, wei_bought);
        emit Snapshot(buyer, address(this).balance, token.balanceOf(address(this)));
        return tokens_bought;
    }

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
    public payable returns (uint256)
    {
        address payable exchange_addr = factory.getExchange(token_addr);
        return tokenToTokenInput(tokens_sold, min_tokens_bought, min_trx_bought, deadline, msg.sender, msg.sender, exchange_addr);
    }

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
    public returns (uint256)
    {
        address payable exchange_addr = factory.getExchange(token_addr);
        return tokenToTokenInput(tokens_sold, min_tokens_bought, min_trx_bought, deadline, msg.sender, recipient, exchange_addr);
    }

    function tokenToTokenOutput(
        uint256 tokens_bought,
        uint256 max_tokens_sold,
        uint256 max_trx_sold,
        uint256 deadline,
        address buyer,
        address recipient,
        address payable exchange_addr)
    nonReentrant
    private returns (uint256)
    {
        require(deadline >= block.timestamp && (tokens_bought > 0 && max_trx_sold > 0), "illegal input parameters");
        require(exchange_addr != address(this) && exchange_addr != address(0), "illegal exchange addr");
        uint256 trx_bought = IJustswapExchange(exchange_addr).getTrxToTokenOutputPrice(tokens_bought);
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 tokens_sold = getOutputPrice(trx_bought, token_reserve, address(this).balance);
        // tokens sold is always > 0
        require(max_tokens_sold >= tokens_sold && max_trx_sold >= trx_bought, "max token sold not matched");

        require(address(token).safeTransferFrom(buyer, address(this), tokens_sold), "transfer failed");
        uint256 trx_sold = IJustswapExchange(exchange_addr).trxToTokenTransferOutput.value(trx_bought)(tokens_bought, deadline, recipient);
        emit TrxPurchase(buyer, tokens_sold, trx_bought);
        emit Snapshot(buyer, address(this).balance, token.balanceOf(address(this)));
        return tokens_sold;
    }

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
    public returns (uint256)
    {
        address payable exchange_addr = factory.getExchange(token_addr);
        return tokenToTokenOutput(tokens_bought, max_tokens_sold, max_trx_sold, deadline, msg.sender, msg.sender, exchange_addr);
    }

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
    public returns (uint256)
    {
        address payable exchange_addr = factory.getExchange(token_addr);
        return tokenToTokenOutput(tokens_bought, max_tokens_sold, max_trx_sold, deadline, msg.sender, recipient, exchange_addr);
    }

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
        address payable exchange_addr)
    public returns (uint256)
    {
        return tokenToTokenInput(tokens_sold, min_tokens_bought, min_trx_bought, deadline, msg.sender, msg.sender, exchange_addr);
    }

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
        address payable exchange_addr)
    public returns (uint256)
    {
        require(recipient != address(this), "illegal recipient");
        return tokenToTokenInput(tokens_sold, min_tokens_bought, min_trx_bought, deadline, msg.sender, recipient, exchange_addr);
    }

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
        address payable exchange_addr)
    public returns (uint256)
    {
        return tokenToTokenOutput(tokens_bought, max_tokens_sold, max_trx_sold, deadline, msg.sender, msg.sender, exchange_addr);
    }

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
        address payable exchange_addr)
    public returns (uint256)
    {
        require(recipient != address(this), "illegal recipient");
        return tokenToTokenOutput(tokens_bought, max_tokens_sold, max_trx_sold, deadline, msg.sender, recipient, exchange_addr);
    }


    /***********************************|
    |         Getter Functions          |
    |__________________________________*/

    /**
     * @notice Public price function for TRX to Token trades with an exact input.
     * @param trx_sold Amount of TRX sold.
     * @return Amount of Tokens that can be bought with input TRX.
     */
    function getTrxToTokenInputPrice(uint256 trx_sold) public view returns (uint256) {
        require(trx_sold > 0, "trx sold must greater than 0");
        uint256 token_reserve = token.balanceOf(address(this));
        return getInputPrice(trx_sold, address(this).balance, token_reserve);
    }

    /**
     * @notice Public price function for TRX to Token trades with an exact output.
     * @param tokens_bought Amount of Tokens bought.
     * @return Amount of TRX needed to buy output Tokens.
     */
    function getTrxToTokenOutputPrice(uint256 tokens_bought) public view returns (uint256) {
        require(tokens_bought > 0, "tokens bought must greater than 0");
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 trx_sold = getOutputPrice(tokens_bought, address(this).balance, token_reserve);
        return trx_sold;
    }

    /**
     * @notice Public price function for Token to TRX trades with an exact input.
     * @param tokens_sold Amount of Tokens sold.
     * @return Amount of TRX that can be bought with input Tokens.
     */
    function getTokenToTrxInputPrice(uint256 tokens_sold) public view returns (uint256) {
        require(tokens_sold > 0, "tokens sold must greater than 0");
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 trx_bought = getInputPrice(tokens_sold, token_reserve, address(this).balance);
        return trx_bought;
    }

    /**
     * @notice Public price function for Token to TRX trades with an exact output.
     * @param trx_bought Amount of output TRX.
     * @return Amount of Tokens needed to buy output TRX.
     */
    function getTokenToTrxOutputPrice(uint256 trx_bought) public view returns (uint256) {
        require(trx_bought > 0, "trx bought must greater than 0");
        uint256 token_reserve = token.balanceOf(address(this));
        return getOutputPrice(trx_bought, token_reserve, address(this).balance);
    }

    /**
     * @return Address of Token that is sold on this exchange.
     */
    function tokenAddress() public view returns (address) {
        return address(token);
    }

    /**
     * @return Address of factory that created this exchange.
     */
    function factoryAddress() public view returns (address) {
        return address(factory);
    }


    /***********************************|
    |        Liquidity Functions        |
    |__________________________________*/

    /**
     * @notice Deposit TRX && Tokens (token) at current ratio to mint JUSTSWAP tokens.
     * @dev min_liquidity does nothing when total JUSTSWAP supply is 0.
     * @param min_liquidity Minimum number of JUSTSWAP sender will mint if total JUSTSWAP supply is greater than 0.
     * @param max_tokens Maximum number of tokens deposited. Deposits max amount if total JUSTSWAP supply is 0.
     * @param deadline Time after which this transaction can no longer be executed.
     * @return The amount of JUSTSWAP minted.
     */
    function addLiquidity(uint256 min_liquidity, uint256 max_tokens, uint256 deadline) public payable nonReentrant returns (uint256) {
        require(deadline > block.timestamp && max_tokens > 0 && msg.value > 0, 'JustExchange#addLiquidity: INVALID_ARGUMENT');
        uint256 total_liquidity = _totalSupply;

        if (total_liquidity > 0) {
            require(min_liquidity > 0, "min_liquidity must greater than 0");
            uint256 trx_reserve = address(this).balance.sub(msg.value);
            uint256 token_reserve = token.balanceOf(address(this));
            uint256 token_amount = (msg.value.mul(token_reserve).div(trx_reserve)).add(1);
            uint256 liquidity_minted = msg.value.mul(total_liquidity).div(trx_reserve);

            require(max_tokens >= token_amount && liquidity_minted >= min_liquidity, "max tokens not meet or liquidity_minted not meet min_liquidity");
            _balances[msg.sender] = _balances[msg.sender].add(liquidity_minted);
            _totalSupply = total_liquidity.add(liquidity_minted);

            require(address(token).safeTransferFrom(msg.sender, address(this), token_amount), "transfer failed");
            emit AddLiquidity(msg.sender, msg.value, token_amount);
            emit Snapshot(msg.sender, address(this).balance, token.balanceOf(address(this)));
            emit Transfer(address(0), msg.sender, liquidity_minted);
            return liquidity_minted;

        } else {
            require(address(factory) != address(0) && address(token) != address(0) && msg.value >= 10_000_000, "INVALID_VALUE");
            require(factory.getExchange(address(token)) == address(this), "token address not meet exchange");
            uint256 token_amount = max_tokens;
            uint256 initial_liquidity = address(this).balance;
            _totalSupply = initial_liquidity;
            _balances[msg.sender] = initial_liquidity;

            require(address(token).safeTransferFrom(msg.sender, address(this), token_amount), "tranfer failed");
            emit AddLiquidity(msg.sender, msg.value, token_amount);
            emit Snapshot(msg.sender, address(this).balance, token.balanceOf(address(this)));
            emit Transfer(address(0), msg.sender, initial_liquidity);
            return initial_liquidity;
        }
    }

    /**
     * @dev Burn JUSTSWAP tokens to withdraw TRX && Tokens at current ratio.
     * @param amount Amount of JUSTSWAP burned.
     * @param min_trx Minimum TRX withdrawn.
     * @param min_tokens Minimum Tokens withdrawn.
     * @param deadline Time after which this transaction can no longer be executed.
     * @return The amount of TRX && Tokens withdrawn.
     */
    function removeLiquidity(uint256 amount, uint256 min_trx, uint256 min_tokens, uint256 deadline) public nonReentrant returns (uint256, uint256) {
        require(amount > 0 && deadline > block.timestamp && min_trx > 0 && min_tokens > 0, "illegal input parameters");
        uint256 total_liquidity = _totalSupply;
        require(total_liquidity > 0, "total_liquidity must greater than 0");
        uint256 token_reserve = token.balanceOf(address(this));
        uint256 trx_amount = amount.mul(address(this).balance) / total_liquidity;
        uint256 token_amount = amount.mul(token_reserve) / total_liquidity;
        require(trx_amount >= min_trx && token_amount >= min_tokens, "min_token or min_trx not meet");

        _balances[msg.sender] = _balances[msg.sender].sub(amount);
        _totalSupply = total_liquidity.sub(amount);
        msg.sender.transfer(trx_amount);

        require(address(token).safeTransfer(msg.sender, token_amount), "transfer failed");
        emit RemoveLiquidity(msg.sender, trx_amount, token_amount);
        emit Snapshot(msg.sender, address(this).balance, token.balanceOf(address(this)));
        emit Transfer(msg.sender, address(0), amount);
        return (trx_amount, token_amount);
    }


}
