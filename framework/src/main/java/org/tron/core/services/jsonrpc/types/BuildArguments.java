package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressCompatibleToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.paramQuantityIsNull;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.paramStringIsNull;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseQuantityValue;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.requireValidHex;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidRequestException;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil.HexMode;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BuildArguments {

  /**
   * Conflict error message wording. Mirrors go-ethereum's
   * {@code setDefaults} verbatim — external EVM tooling may
   * pattern-match this string. Do not change the wording without
   * coordinating with downstream consumers.
   */
  private static final String CONFLICT_ERR_MSG =
      "both \"data\" and \"input\" are set and not equal. "
          + "Please use \"input\" to pass transaction call data";

  @Getter
  @Setter
  private String from;
  @Getter
  @Setter
  private String to;
  @Getter
  @Setter
  private String gas = "0x0";
  @Getter
  @Setter
  private String gasPrice = ""; //not used
  @Getter
  @Setter
  private String value;
  @Getter
  @Setter
  private String data;
  @Getter
  @Setter
  private String input;
  @Getter
  @Setter
  private String nonce = ""; //not used

  @Getter
  @Setter
  private Long tokenId = 0L;
  @Getter
  @Setter
  private Long tokenValue = 0L;
  @Getter
  @Setter
  private String abi = "";
  @Getter
  @Setter
  private Long consumeUserResourcePercent = 0L;
  @Getter
  @Setter
  private Long originEnergyLimit = 0L;
  @Getter
  @Setter
  private String name = "";

  @Getter
  @Setter
  private Integer permissionId = 0;
  @Getter
  @Setter
  private String extraData = "";

  @Getter
  @Setter
  private boolean visible = false;

  public BuildArguments(CallArguments args) {
    from = args.getFrom();
    to = args.getTo();
    gas = args.getGas();
    gasPrice = args.getGasPrice();
    value = args.getValue();
    data = args.getData();
    input = args.getInput();
  }

  /**
   * Returns {@code input} if non-null, else {@code data}. Pure
   * precedence resolution, mirroring go-ethereum's
   * {@code TransactionArgs.data()}.
   *
   * <p>Both fields are first validated by
   * {@link org.tron.core.services.jsonrpc.JsonRpcApiUtil#requireValidHex}
   * — strict for {@code input}, lenient for {@code data} (see that
   * method for the rules).
   *
   * <p>Conflict between {@code input} and {@code data} is not checked
   * here. Build-path callers must route through
   * {@link #getContractType(Wallet)} for the geth-equivalent
   * {@code setDefaults} enforcement.
   *
   * <p>Java callers using positional constructors should pass
   * {@code null} (not {@code ""}) for unset {@code input}.
   *
   * <p>Verb-prefix name (not {@code getXxx}) keeps Jackson and
   * FastJSON's JavaBean introspection from invoking it during
   * serialisation; two regression tests per DTO pin this invariant.
   */
  public String resolveData() throws JsonRpcInvalidParamsException {
    requireValidHex("input", input, HexMode.STRICT);
    requireValidHex("data", data, HexMode.LENIENT);
    return input != null ? input : data;
  }

  public ContractType getContractType(Wallet wallet) throws JsonRpcInvalidRequestException,
      JsonRpcInvalidParamsException {
    // Fail fast on bad hex / conflict before the state lookup;
    // calldataEquals relies on resolveData() having validated hex first.
    String resolvedData = resolveData();
    validateCallDataConflict();

    ContractType contractType;

    // to is null
    if (paramStringIsNull(to)) {
      // data is null
      if (paramStringIsNull(resolvedData)) {
        throw new JsonRpcInvalidRequestException("invalid json request");
      }

      contractType = ContractType.CreateSmartContract;
    } else {
      // to is not null
      byte[] contractAddressData = addressCompatibleToByteArray(to);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(contractAddressData)).build();
      SmartContract smartContract = wallet.getContract(bytesMessage);

      // check if to is smart contract
      if (smartContract != null) {
        contractType = ContractType.TriggerSmartContract;
      } else {
        // tokenId and tokenValue: trc10, value: TRX
        if (availableTransferAsset()) {
          contractType = ContractType.TransferAssetContract;
        } else {
          if (StringUtils.isNotEmpty(value)) {
            contractType = ContractType.TransferContract;
          } else {
            throw new JsonRpcInvalidRequestException("invalid json request");
          }
        }
      }
    }

    return contractType;
  }

  public long parseValue() throws JsonRpcInvalidParamsException {
    return parseQuantityValue(value);
  }

  public long parseGas() throws JsonRpcInvalidParamsException {
    return parseQuantityValue(gas);
  }

  private boolean availableTransferAsset() {
    return tokenId > 0 && tokenValue > 0 && paramQuantityIsNull(value);
  }

  /**
   * Throws when both fields decode to non-equal bytes. Wording matches
   * geth's setDefaults so existing tooling can detect the error string.
   */
  private void validateCallDataConflict() throws JsonRpcInvalidParamsException {
    if (input != null && data != null && !calldataEquals(input, data)) {
      throw new JsonRpcInvalidParamsException(CONFLICT_ERR_MSG);
    }
  }

  /**
   * Byte-level equality, so {@code "0xDEAD"} equals {@code "0xdead"}. Both
   * args must have passed {@code requireValidHex} first.
   */
  private static boolean calldataEquals(String a, String b) {
    return Arrays.equals(ByteArray.fromHexString(a), ByteArray.fromHexString(b));
  }

}