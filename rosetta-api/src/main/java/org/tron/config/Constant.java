package org.tron.config;

import java.util.Arrays;
import java.util.List;
import org.tron.model.Error;
import org.tron.model.OperationStatus;

public class Constant {

  public static final String rosettaVersion = "1.4.0";
  public static final String middlewareVersion = "1.0.2";

  // OperationStatus
  public static OperationStatus OPERATION_SUCCESS = new OperationStatus().status("SUCCESS").successful(true);
  public static OperationStatus OPERATION_REVERT = new OperationStatus().status("REVERTED").successful(false);

  // errors
  public static Error INVALID_ACCOUNT_FORMAT =
          new Error().code(12).message("Invalid account format").retriable(true).details(null);
  public static Error INVALID_TRANSACTION_FORMAT =
          new Error().code(100).message("Invalid transaction format").retriable(false).details(null);


  public static String[] supportOperationTypes = new String[]{
          "TRANSFER"
  };
  public static List<OperationStatus> supportOperationStatuses = Arrays.asList(
          OPERATION_SUCCESS,
          OPERATION_REVERT
  );
  public static List<Error> supportErrors = Arrays.asList(
          INVALID_ACCOUNT_FORMAT,
          INVALID_TRANSACTION_FORMAT
  );
}
