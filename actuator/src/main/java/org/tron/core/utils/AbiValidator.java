package org.tron.core.utils;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.EntryType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.Param;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType;

public final class AbiValidator {

  private static final Pattern INT_TYPE = Pattern.compile("^(u?int)(\\d*)$");
  private static final Pattern BYTES_N_TYPE = Pattern.compile("^bytes(\\d+)$");
  private static final Pattern ARRAY_SUFFIX = Pattern.compile("\\[(\\d*)]$");

  private static final Set<String> BASE_TYPES = ImmutableSet.of(
      "address", "bool", "string", "bytes", "function", "tuple", "trcToken");

  private AbiValidator() {
  }

  public static void validate(ABI abi) throws ContractValidateException {
    if (abi == null || abi.getEntrysCount() == 0) {
      return;
    }

    int constructorCount = 0;
    int fallbackCount = 0;
    int receiveCount = 0;

    for (int i = 0; i < abi.getEntrysCount(); i++) {
      Entry entry = abi.getEntrys(i);
      EntryType type = entry.getType();

      if (type == EntryType.UnknownEntryType || type == EntryType.UNRECOGNIZED) {
        throw new ContractValidateException(
            String.format("abi entry #%d: unknown entry type", i));
      }

      switch (type) {
        case Constructor:
          constructorCount++;
          break;
        case Fallback:
          fallbackCount++;
          if (entry.getInputsCount() > 0 || entry.getOutputsCount() > 0) {
            throw new ContractValidateException(String.format(
                "abi entry #%d: fallback function must not have inputs or outputs", i));
          }
          break;
        case Receive:
          receiveCount++;
          if (entry.getInputsCount() > 0 || entry.getOutputsCount() > 0) {
            throw new ContractValidateException(String.format(
                "abi entry #%d: receive function must not have inputs or outputs", i));
          }
          if (entry.getStateMutability() != StateMutabilityType.Payable && !entry.getPayable()) {
            throw new ContractValidateException(String.format(
                "abi entry #%d: receive function must be payable", i));
          }
          break;
        case Function:
          if (entry.getName().isEmpty()) {
            throw new ContractValidateException(String.format(
                "abi entry #%d: function must have a name", i));
          }
          break;
        case Event:
          if (entry.getName().isEmpty() && !entry.getAnonymous()) {
            throw new ContractValidateException(String.format(
                "abi entry #%d: non-anonymous event must have a name", i));
          }
          break;
        case Error:
          if (entry.getName().isEmpty()) {
            throw new ContractValidateException(String.format(
                "abi entry #%d: error must have a name", i));
          }
          break;
        default:
          break;
      }

      validateParams(i, "inputs", entry.getInputsList());
      validateParams(i, "outputs", entry.getOutputsList());
    }

    if (constructorCount > 1) {
      throw new ContractValidateException("abi: only one constructor is allowed");
    }
    if (fallbackCount > 1) {
      throw new ContractValidateException("abi: only one fallback function is allowed");
    }
    if (receiveCount > 1) {
      throw new ContractValidateException("abi: only one receive function is allowed");
    }
  }

  private static void validateParams(int entryIdx, String side, List<Param> params)
      throws ContractValidateException {
    for (int j = 0; j < params.size(); j++) {
      String type = params.get(j).getType();
      String reason = checkType(type);
      if (reason != null) {
        throw new ContractValidateException(String.format(
            "abi entry #%d %s[%d] type '%s': %s", entryIdx, side, j, type, reason));
      }
    }
  }

  // Returns null when the type is acceptable, otherwise a short failure reason.
  private static String checkType(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "type must not be empty";
    }
    if (!raw.equals(raw.trim())) {
      return "type must not contain leading/trailing whitespace";
    }
    String t = raw;

    while (true) {
      Matcher m = ARRAY_SUFFIX.matcher(t);
      if (!m.find()) {
        break;
      }
      String n = m.group(1);
      if (!n.isEmpty()) {
        long size;
        try {
          size = Long.parseLong(n);
        } catch (NumberFormatException nfe) {
          return "malformed array size";
        }
        if (size <= 0) {
          return "array size must be positive";
        }
      }
      t = t.substring(0, t.length() - m.group().length());
    }

    if (t.indexOf('[') >= 0 || t.indexOf(']') >= 0) {
      return "malformed array brackets";
    }

    if (BASE_TYPES.contains(t)) {
      return null;
    }

    Matcher mi = INT_TYPE.matcher(t);
    if (mi.matches()) {
      String width = mi.group(2);
      if (width.isEmpty()) {
        return "shorthand uint/int is not allowed, use uintN/intN";
      }
      int w;
      try {
        w = Integer.parseInt(width);
      } catch (NumberFormatException nfe) {
        return "invalid integer width";
      }
      if (w < 8 || w > 256 || (w % 8) != 0) {
        return "integer width must be a multiple of 8 in [8, 256]";
      }
      return null;
    }

    Matcher mb = BYTES_N_TYPE.matcher(t);
    if (mb.matches()) {
      int n;
      try {
        n = Integer.parseInt(mb.group(1));
      } catch (NumberFormatException nfe) {
        return "invalid bytesN size";
      }
      if (n < 1 || n > 32) {
        return "bytesN size must be in [1, 32]";
      }
      return null;
    }

    if (t.startsWith("fixed") || t.startsWith("ufixed")) {
      return "fixed/ufixed types are not supported";
    }

    return "unknown base type";
  }
}
