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

  // errors
  public static Error INVALID_ACCOUNT_FORMAT =
          new Error().code(12).message("Invalid account format").retriable(true).details(null);


  public static String[] supportOperationTypes = new String[]{
          "TRANSFER"
  };
  public static List<OperationStatus> supportOperationStatuses = Arrays.asList(
          OPERATION_SUCCESS
  );
  public static List<Error> supportErrors = Arrays.asList(
          INVALID_ACCOUNT_FORMAT
  );
}
