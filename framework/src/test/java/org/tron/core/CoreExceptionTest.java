package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.tron.core.exception.BadBlockException.TypeEnum.CALC_MERKLE_ROOT_FAILED;
import static org.tron.core.exception.BadBlockException.TypeEnum.DEFAULT;
import static org.tron.core.exception.P2pException.TypeEnum.NO_SUCH_MESSAGE;
import static org.tron.core.exception.P2pException.TypeEnum.PARSE_MESSAGE_FAILED;
import static org.tron.core.exception.P2pException.TypeEnum.SYNC_FAILED;

import org.junit.Test;
import org.tron.common.error.TronDBException;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.EventBloomException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.HighFreqException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.core.exception.JsonRpcMethodNotFoundException;
import org.tron.core.exception.JsonRpcTooManyResultException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.PermissionException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.core.exception.SignatureFormatException;
import org.tron.core.exception.StoreException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TraitorPeerException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.TronRuntimeException;
import org.tron.core.exception.TypeMismatchNamingException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.UnReachBlockException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZkProofValidateException;
import org.tron.core.exception.ZksnarkException;

public class CoreExceptionTest {

  @Test
  public void testAccountResourceInsufficientExceptionMessage() {
    String expectedMessage = "Account resources are insufficient";
    AccountResourceInsufficientException exception =
        new AccountResourceInsufficientException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  public void testBadBlockExceptionWithNoMessageAndDefaultType() {
    BadBlockException exception = new BadBlockException();

    assertNull(exception.getMessage());

    assertEquals(DEFAULT, exception.getType());
  }

  @Test
  public void testBadBlockExceptionWithMessageAndDefaultType() {
    String testMessage = "Block is bad due to some reason";

    BadBlockException exception = new BadBlockException(testMessage);

    assertEquals(testMessage, exception.getMessage());

    assertEquals(DEFAULT, exception.getType());
  }

  @Test
  public void testBadBlockExceptionWithSpecificTypeAndMessage() {
    String testMessage = "Failed to calculate Merkle root";

    BadBlockException exception = new BadBlockException(CALC_MERKLE_ROOT_FAILED, testMessage);

    assertEquals(testMessage, exception.getMessage());

    assertEquals(CALC_MERKLE_ROOT_FAILED, exception.getType());
  }

  @Test
  public void testTypeEnumValues() {
    assertEquals(Integer.valueOf(1), CALC_MERKLE_ROOT_FAILED.getValue());

    assertEquals(Integer.valueOf(100), DEFAULT.getValue());
  }

  @Test
  public void testBadItemExceptionDefaultConstructor() {
    BadItemException exception = new BadItemException();
    assertNotNull(exception);
  }

  @Test
  public void testBadItemExceptionMessageConstructor() {
    String expectedMessage = "This item is bad!";
    BadItemException exception = new BadItemException(expectedMessage);
    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
    assertNotNull(exception);
  }

  @Test
  public void testBadItemExceptionMessageAndCauseConstructor() {
    String expectedMessage = "This item is really bad!";
    Throwable expectedCause = new Throwable("Some underlying cause");
    BadItemException exception = new BadItemException(expectedMessage, expectedCause);
    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(expectedCause, exception.getCause());
    assertNotNull(exception);
  }

  @Test
  public void testBadNumberBlockExceptionDefaultConstructor() {
    BadNumberBlockException exception = new BadNumberBlockException();
    assertNull(exception.getMessage());
    assertNotNull(exception);
  }

  @Test
  public void testBadNumberBlockExceptionMessageConstructor() {
    String expectedMessage = "Number block is bad!";
    BadNumberBlockException exception = new BadNumberBlockException(expectedMessage);
    assertEquals(expectedMessage, exception.getMessage());
    assertNotNull(exception);
  }

  @Test
  public void testBadTransactionExceptionDefaultConstructor() {
    BadTransactionException exception = new BadTransactionException();
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
    assertNotNull(exception);
  }

  @Test
  public void testBadTransactionExceptionMessageConstructor() {
    String expectedMessage = "Transaction is bad!";
    BadTransactionException exception = new BadTransactionException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());

    assertNull(exception.getCause());

    assertNotNull(exception);
  }

  @Test
  public void testBadTransactionExceptionMessageAndCauseConstructor() {
    String expectedMessage = "Transaction failed due to an error";
    Throwable cause = new IllegalArgumentException("Invalid transaction data");

    BadTransactionException exception = new BadTransactionException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());

    assertNotNull(exception);
  }

  @Test
  public void testBalanceInsufficientExceptionWithNoMessage() {
    BalanceInsufficientException exception = new BalanceInsufficientException();
    assertNull(exception.getMessage());
  }

  @Test
  public void testBalanceInsufficientExceptionWithMessage() {
    String testMessage = "Balance is insufficient for this transaction";
    BalanceInsufficientException exception = new BalanceInsufficientException(testMessage);
    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testCancelExceptionWithNoMessage() {
    CancelException exception = new CancelException();

    assertNull(exception.getMessage());
  }

  @Test
  public void testCancelExceptionWithMessage() {
    String testMessage = "Operation canceled by user";

    CancelException exception = new CancelException(testMessage);

    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testCipherExceptionWithMessage() {
    String testMessage = "Cipher operation failed";

    CipherException exception = new CipherException(testMessage);

    assertEquals(testMessage, exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  public void testCipherExceptionWithCause() {
    Throwable testCause = new Throwable("Underlying error");

    CipherException exception = new CipherException(testCause);

    assertSame(testCause, exception.getCause());

  }

  @Test
  public void testCipherExceptionWithMessageAndCause() {
    String testMessage = "Cipher operation failed due to error";

    Throwable testCause = new Throwable("Underlying error");
    CipherException exception = new CipherException(testMessage, testCause);

    assertEquals(testMessage, exception.getMessage());

    assertSame(testCause, exception.getCause());
  }

  @Test
  public void testContractExeExceptionWithNoMessage() {
    ContractExeException exception = new ContractExeException();

    assertNull(exception.getMessage());
  }

  @Test
  public void testContractExeExceptionWithMessage() {
    String testMessage = "Contract execution failed";

    ContractExeException exception = new ContractExeException(testMessage);

    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testContractSizeNotEqualToOneExceptionWithNoMessage() {
    ContractSizeNotEqualToOneException exception = new ContractSizeNotEqualToOneException();

    assertNull(exception.getMessage());
  }

  @Test
  public void testContractSizeNotEqualToOneExceptionWithMessage() {
    String testMessage = "Contract size is not equal to one";

    ContractSizeNotEqualToOneException exception =
        new ContractSizeNotEqualToOneException(testMessage);

    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testContractValidateExceptionWithNoMessageOrThrowable() {
    ContractValidateException exception = new ContractValidateException();

    assertNull(exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  public void testContractValidateExceptionWithMessage() {
    String testMessage = "Contract validation failed";

    ContractValidateException exception = new ContractValidateException(testMessage);

    assertEquals(testMessage, exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  public void testContractValidateExceptionWithMessageAndThrowable() {
    String testMessage = "Contract validation failed due to internal error";

    Throwable cause = new RuntimeException("Internal error");

    ContractValidateException exception = new ContractValidateException(testMessage, cause);

    assertEquals(testMessage, exception.getMessage());

    assertSame(cause, exception.getCause());
  }

  @Test
  public void testDupTransactionExceptionDefaultConstructor() {
    DupTransactionException exception = new DupTransactionException();

    assertNotNull(exception);

  }

  @Test
  public void testDupTransactionExceptionMessageConstructor() {
    String testMessage = "Duplicate Transaction Exception";
    DupTransactionException exception = new DupTransactionException(testMessage);

    assertNotNull(exception);

    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testEventBloomExceptionDefaultConstructor() {
    EventBloomException exception = new EventBloomException();

    assertNotNull(exception);

    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testEventBloomExceptionMessageConstructor() {
    String testMessage = "Event Bloom Exception occurred";
    EventBloomException exception = new EventBloomException(testMessage);

    assertNotNull(exception);

    assertEquals(testMessage, exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  public void testEventBloomExceptionMessageAndCauseConstructor() {
    String testMessage = "Event Bloom Exception with cause";
    Throwable testCause = new Throwable("Root cause");
    EventBloomException exception = new EventBloomException(testMessage, testCause);

    assertNotNull(exception);

    assertEquals(testMessage, exception.getMessage());

    assertEquals(testCause, exception.getCause());
  }

  @Test
  public void testHeaderNotFoundDefaultConstructor() {
    HeaderNotFound exception = new HeaderNotFound();

    assertNotNull(exception);

    assertNull(exception.getMessage());
  }

  @Test
  public void testHeaderNotFoundMessageConstructor() {
    String testMessage = "Header not found";
    HeaderNotFound exception = new HeaderNotFound(testMessage);

    assertNotNull(exception);

    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testHighFreqExceptionDefaultConstructor() {
    HighFreqException exception = new HighFreqException();

    assertNotNull("Exception object should not be null", exception);

    assertNull("Exception message should be null", exception.getMessage());
  }

  @Test
  public void testHighFreqExceptionMessageConstructor() {
    String testMessage = "High frequency error occurred";
    HighFreqException exception = new HighFreqException(testMessage);

    assertNotNull("Exception object should not be null", exception);

    assertEquals("Exception message should match the provided message",
        testMessage, exception.getMessage());
  }

  @Test
  public void testItemNotFoundExceptionWithMessage() {
    String testMessage = "Item not found";
    ItemNotFoundException exception = new ItemNotFoundException(testMessage);

    assertNotNull("Exception object should not be null", exception);
    assertEquals("Exception message should match the provided message", testMessage,
        exception.getMessage());
    assertNull("Cause should be null when not provided", exception.getCause());
  }

  @Test
  public void testItemNotFoundExceptionDefaultConstructor() {
    ItemNotFoundException exception = new ItemNotFoundException();

    assertNotNull("Exception object should not be null", exception);
    assertNull("Exception message should be null when no message is provided",
        exception.getMessage());
    assertNull("Cause should be null when not provided", exception.getCause());
  }

  @Test
  public void testItemNotFoundExceptionWithMessageAndCause() {
    String testMessage = "Item not found in database";
    Throwable testCause = new Throwable("Database error");
    ItemNotFoundException exception = new ItemNotFoundException(testMessage, testCause);

    assertNotNull("Exception object should not be null", exception);
    assertEquals("Exception message should match the provided message",
        testMessage, exception.getMessage());
    assertEquals("Cause should match the provided Throwable", testCause, exception.getCause());
  }

  @Test
  public void testJsonRpcInternalExceptionDefaultConstructor() {
    JsonRpcInternalException exception = new JsonRpcInternalException();

    assertNotNull("Exception object should not be null", exception);
    assertNull("Exception message should be null when no message is provided",
        exception.getMessage());
    assertNull("Cause should be null when not provided", exception.getCause());
  }

  @Test
  public void testJsonRpcInternalExceptionWithMessage() {
    String testMessage = "Internal JSON-RPC error occurred";
    JsonRpcInternalException exception = new JsonRpcInternalException(testMessage);

    assertNotNull("Exception object should not be null", exception);
    assertEquals("Exception message should match the provided message", testMessage,
        exception.getMessage());
    assertNull("Cause should be null when not provided", exception.getCause());
  }

  @Test
  public void testJsonRpcInternalExceptionWithMessageAndCause() {
    String testMessage = "Internal JSON-RPC processing failed";
    Throwable testCause = new Throwable("Underlying system error");
    JsonRpcInternalException exception = new JsonRpcInternalException(testMessage, testCause);

    assertNotNull("Exception object should not be null", exception);
    assertEquals("Exception message should match the provided message",
        testMessage, exception.getMessage());
    assertEquals("Cause should match the provided Throwable", testCause, exception.getCause());
  }

  @Test
  public void testJsonRpcInvalidParamsExceptionDefaultConstructor() {
    JsonRpcInvalidParamsException exception = new JsonRpcInvalidParamsException();

    assertNotNull("Exception object should not be null", exception);
    assertNull("Exception message should be null when no message is provided",
        exception.getMessage());
    assertNull("Cause should be null when not provided", exception.getCause());
  }

  @Test
  public void testJsonRpcInvalidParamsExceptionWithMessage() {
    String testMessage = "Invalid parameters provided";
    JsonRpcInvalidParamsException exception = new JsonRpcInvalidParamsException(testMessage);

    assertNotNull("Exception object should not be null", exception);
    assertEquals("Exception message should match the provided message",
        testMessage, exception.getMessage());
    assertNull("Cause should be null when not provided", exception.getCause());
  }

  @Test
  public void testJsonRpcInvalidParamsExceptionWithMessageAndCause() {
    String testMessage = "Parameter validation failed";
    Throwable testCause = new Throwable("Underlying validation error");
    JsonRpcInvalidParamsException e = new JsonRpcInvalidParamsException(testMessage, testCause);

    assertNotNull("Exception object should not be null", e);
    assertEquals("Exception message should match the provided message", testMessage,
        e.getMessage());
    assertEquals("Cause should match the provided Throwable", testCause, e.getCause());
  }

  @Test
  public void testJsonRpcInvalidRequestExceptionDefaultConstructor() {
    JsonRpcInvalidRequestException exception = new JsonRpcInvalidRequestException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testJsonRpcInvalidRequestExceptionConstructorWithMessage() {
    String testMessage = "Invalid JSON-RPC request";
    JsonRpcInvalidRequestException exception = new JsonRpcInvalidRequestException(testMessage);
    assertNotNull(exception);
    assertEquals(testMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testJsonRpcInvalidRequestExceptionConstructorWithMessageAndCause() {
    String testMessage = "Invalid JSON-RPC request with cause";
    Throwable testCause = new Throwable("Root cause");
    JsonRpcInvalidRequestException e = new JsonRpcInvalidRequestException(testMessage, testCause);
    assertNotNull(e);
    assertEquals(testMessage, e.getMessage());
    assertEquals(testCause, e.getCause());
  }

  @Test
  public void testJsonRpcMethodNotFoundExceptionDefaultConstructor() {
    JsonRpcMethodNotFoundException exception = new JsonRpcMethodNotFoundException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testJsonRpcMethodNotFoundExceptionConstructorWithMessage() {
    String testMessage = "JSON-RPC method not found";
    JsonRpcMethodNotFoundException exception = new JsonRpcMethodNotFoundException(testMessage);
    assertNotNull(exception);
    assertEquals(testMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testJsonRpcMethodNotFoundExceptionConstructorWithMessageAndCause() {
    String testMessage = "JSON-RPC method not found with cause";
    Throwable testCause = new Throwable("Root cause");
    JsonRpcMethodNotFoundException e = new JsonRpcMethodNotFoundException(testMessage, testCause);
    assertNotNull(e);
    assertEquals(testMessage, e.getMessage());
    assertEquals(testCause, e.getCause());
  }

  @Test
  public void testJsonRpcTooManyResultExceptionDefaultConstructor() {
    JsonRpcTooManyResultException exception = new JsonRpcTooManyResultException();
    assertNotNull("Exception object should not be null", exception);
    assertNull(exception.getMessage());
    assertNull("Cause should be null for default constructor", exception.getCause());
  }

  @Test
  public void testJsonRpcTooManyResultExceptionWithMessage() {
    String testMessage = "Too many results returned by JSON-RPC method";
    JsonRpcTooManyResultException exception = new JsonRpcTooManyResultException(testMessage);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the provided message", testMessage, exception.getMessage());
    assertNull("Cause should be null when only message is provided", exception.getCause());
  }

  @Test
  public void testJsonRpcTooManyResultExceptionWithMessageAndCause() {
    String testMessage = "Too many results returned with cause";
    Throwable testCause = new Throwable("Root cause");
    JsonRpcTooManyResultException e = new JsonRpcTooManyResultException(testMessage, testCause);
    assertNotNull("Exception object should not be null", e);
    assertEquals("Message should match the provided message", testMessage, e.getMessage());
    assertEquals("Cause should match the provided cause", testCause, e.getCause());
  }

  @Test
  public void testNonCommonBlockExceptionDefaultConstructor() {
    NonCommonBlockException exception = new NonCommonBlockException();
    assertNotNull("Exception object should not be null", exception);
    assertNull("Message should be null for default constructor", exception.getMessage());
    assertNull("Cause should be null for default constructor", exception.getCause());
  }

  @Test
  public void testNonCommonBlockExceptionWithMessage() {
    String testMessage = "Block is not common";
    NonCommonBlockException exception = new NonCommonBlockException(testMessage);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the provided message", testMessage, exception.getMessage());
    assertNull("Cause should be null when only message is provided", exception.getCause());
  }

  @Test
  public void testNonCommonBlockExceptionWithMessageAndCause() {
    String testMessage = "Block is not common due to some error";
    Throwable testCause = new Throwable("Root cause of the error");
    NonCommonBlockException exception = new NonCommonBlockException(testMessage, testCause);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the provided message", testMessage, exception.getMessage());
    assertEquals("Cause should match the provided cause", testCause, exception.getCause());
  }

  @Test
  public void testDefaultConstructor() {
    NonUniqueObjectException exception = new NonUniqueObjectException();
    assertNotNull("Exception object should not be null", exception);
    assertNull("Message should be null for default constructor", exception.getMessage());
    assertNull("Cause should be null for default constructor", exception.getCause());
  }

  @Test
  public void testConstructorWithMessage() {
    String testMessage = "Object is not unique";
    NonUniqueObjectException exception = new NonUniqueObjectException(testMessage);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the provided message", testMessage, exception.getMessage());
    assertNull("Cause should be null when only message is provided", exception.getCause());
  }

  @Test
  public void testConstructorWithMessageAndCause() {
    String testMessage = "Object is not unique due to a conflict";
    Throwable testCause = new Throwable("Conflict error");
    NonUniqueObjectException exception = new NonUniqueObjectException(testMessage, testCause);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the provided message", testMessage, exception.getMessage());
    assertEquals("Cause should match the provided cause", testCause, exception.getCause());
  }

  @Test
  public void testConstructorWithCauseOnly() {
    Throwable testCause = new Throwable("Root cause of non-uniqueness");
    NonUniqueObjectException exception = new NonUniqueObjectException(testCause);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should be empty string when only cause is provided", "",
        exception.getMessage());
    assertEquals("Cause should match the provided cause", testCause, exception.getCause());
  }

  @Test
  public void testConstructorWithTypeEnumAndErrMsg() {
    P2pException exception = new P2pException(NO_SUCH_MESSAGE, "Test error message");
    assertNotNull("Exception should not be null", exception);
    assertEquals("Error message should match", "Test error message", exception.getMessage());
    assertEquals("Exception type should be NO_SUCH_MESSAGE", NO_SUCH_MESSAGE, exception.getType());
  }

  @Test
  public void testConstructorWithTypeEnumAndThrowable() {
    Throwable cause = new Throwable("Cause of the error");
    P2pException exception = new P2pException(PARSE_MESSAGE_FAILED, cause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Cause should match", cause, exception.getCause());
  }

  @Test
  public void testConstructorWithTypeEnumErrMsgAndThrowable() {
    Throwable cause = new Throwable("Cause of the error");
    P2pException exception = new P2pException(SYNC_FAILED, "Test error message", cause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Error message should match",
        "Test error message", exception.getMessage());
    assertEquals("Cause should match", cause, exception.getCause());
    assertEquals("Exception type should be SYNC_FAILED", SYNC_FAILED, exception.getType());
  }

  @Test
  public void testP2pExceptionTypeEnumValues() {
    assertNotNull(NO_SUCH_MESSAGE.toString());
    assertEquals("NO_SUCH_MESSAGE value should be 1",
        Integer.valueOf(1), NO_SUCH_MESSAGE.getValue());
    assertEquals("NO_SUCH_MESSAGE desc should be 'no such message'",
        "no such message", NO_SUCH_MESSAGE.getDesc());

    assertEquals("DEFAULT value should be 100", Integer.valueOf(100),
        P2pException.TypeEnum.DEFAULT.getValue());
    assertEquals("DEFAULT desc should be 'default exception'",
        "default exception", P2pException.TypeEnum.DEFAULT.getDesc());
  }

  @Test
  public void testPermissionExceptionDefaultConstructor() {
    PermissionException exception = new PermissionException();
    assertNotNull(exception);
  }

  @Test
  public void testPermissionExceptionWithMessage() {
    String errorMessage = "You do not have sufficient permissions to perform this operation";
    PermissionException exception = new PermissionException(errorMessage);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  public void testReceiptCheckErrExceptionDefaultConstructor() {
    ReceiptCheckErrException exception = new ReceiptCheckErrException();
    assertNotNull("Exception object should not be null", exception);
    assertNull("Message should be null for default constructor", exception.getMessage());
    assertNull("Cause should be null for default constructor", exception.getCause());
  }

  @Test
  public void testReceiptCheckErrExceptionConstructorWithMessage() {
    String errorMessage = "Receipt check failed due to invalid data";
    ReceiptCheckErrException exception = new ReceiptCheckErrException(errorMessage);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the input message", errorMessage, exception.getMessage());
    assertNull("Cause should be null when only message is provided", exception.getCause());
  }

  @Test
  public void testReceiptCheckErrExceptionConstructorWithMessageAndCause() {
    String errorMessage = "Receipt check error";
    Throwable cause = new Throwable("Underlying database error");
    ReceiptCheckErrException exception = new ReceiptCheckErrException(errorMessage, cause);
    assertNotNull("Exception object should not be null", exception);
    assertEquals("Message should match the input message", errorMessage, exception.getMessage());
    assertEquals("Cause should match the input cause", cause, exception.getCause());
  }

  @Test
  public void testRevokingStoreIllegalStateExceptionDefaultConstructor() {
    RevokingStoreIllegalStateException exception = new RevokingStoreIllegalStateException();
    assertNotNull("Exception should not be null", exception);
    assertNull("Message should be null", exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testRevokingStoreIllegalStateExceptionConstructorWithMessage() {
    String errorMessage = "Invalid state for revoking store";
    RevokingStoreIllegalStateException e = new RevokingStoreIllegalStateException(errorMessage);
    assertNotNull("Exception should not be null", e);
    assertEquals("Message should match the input message", errorMessage, e.getMessage());
    assertNull("Cause should be null", e.getCause());
  }

  @Test
  public void testRevokingStoreIllegalStateExceptionConstructorWithMessageAndCause() {
    String errorMessage = "Error occurred during revocation";
    Throwable cause = new Throwable("Database connection failed");
    RevokingStoreIllegalStateException e =
        new RevokingStoreIllegalStateException(errorMessage, cause);
    assertNotNull("Exception should not be null", e);
    assertEquals("Message should match the input message", errorMessage, e.getMessage());
    assertEquals("Cause should match the input cause", cause, e.getCause());
  }

  @Test
  public void testRevokingStoreIllegalStateExceptionConstructorWithCauseOnly() {
    Throwable cause = new Throwable("Unknown error");
    RevokingStoreIllegalStateException exception = new RevokingStoreIllegalStateException(cause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should be empty string", "", exception.getMessage());
    assertEquals("Cause should match the input cause", cause, exception.getCause());
  }

  @Test
  public void testRevokingStoreIllegalStateExceptionConstructorWithActiveSession() {
    int activeSession = 0;
    RevokingStoreIllegalStateException e = new RevokingStoreIllegalStateException(activeSession);
    assertNotNull("Exception should not be null", e);
    assertEquals("Message should indicate activeSession is not greater than 0",
        "activeSession 0 has to be greater than 0", e.getMessage());
  }

  @Test
  public void testSignatureFormatExceptionDefaultConstructor() {
    SignatureFormatException exception = new SignatureFormatException();
    assertNotNull("Exception should not be null", exception);
    assertNull("Message should be null", exception.getMessage());
  }

  @Test
  public void testSignatureFormatExceptionWithMessage() {
    String errorMessage = "Invalid signature format";
    SignatureFormatException exception = new SignatureFormatException(errorMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match the provided error message",
        errorMessage, exception.getMessage());
  }

  @Test
  public void testStoreExceptionDefaultConstructor() {
    StoreException exception = new StoreException();
    assertNotNull("Exception should not be null", exception);
    assertNull("Message should be null", exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testStoreExceptionWithMessage() {
    String errorMessage = "Store error occurred";
    StoreException exception = new StoreException(errorMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match the provided error message",
        errorMessage, exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testStoreExceptionWithMessageAndCause() {
    String errorMessage = "Store error occurred";
    Throwable cause = new Throwable("Root cause");
    StoreException exception = new StoreException(errorMessage, cause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match the provided error message",
        errorMessage, exception.getMessage());
    assertEquals("Cause should match the provided cause", cause, exception.getCause());
  }

  @Test
  public void testStoreExceptionWithCause() {
    Throwable cause = new Throwable("Root cause");
    StoreException exception = new StoreException(cause);
    assertNotNull("Exception should not be null", exception);
  }


  @Test
  public void testTaposExceptionDefaultConstructor() {
    TaposException exception = new TaposException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testTaposExceptionWithMessage() {
    String errorMessage = "Tapos error occurred";
    TaposException exception = new TaposException(errorMessage);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testTaposExceptionWithMessageAndCause() {
    String errorMessage = "Tapos error occurred";
    Throwable cause = new Throwable("Root cause");
    TaposException exception = new TaposException(errorMessage, cause);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testTooBigTransactionExceptionDefaultConstructor() {
    TooBigTransactionException exception = new TooBigTransactionException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
  }

  @Test
  public void testTooBigTransactionExceptionWithMessage() {
    String errorMessage = "Transaction is too big";
    TooBigTransactionException exception = new TooBigTransactionException(errorMessage);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  public void testTooBigTransactionResultExceptionDefaultConstructor() {
    TooBigTransactionResultException exception = new TooBigTransactionResultException();
    assertNotNull(exception);
    assertEquals("too big transaction result",
        exception.getMessage());
  }

  @Test
  public void testTooBigTransactionResultExceptionWithMessage() {
    String customMessage = "Custom error message for too big transaction result";
    TooBigTransactionResultException e = new TooBigTransactionResultException(customMessage);
    assertNotNull(e);
    assertEquals(customMessage, e.getMessage());
  }

  @Test
  public void testTraitorPeerExceptionDefaultConstructor() {
    TraitorPeerException exception = new TraitorPeerException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testTraitorPeerExceptionWithMessage() {
    String errorMessage = "Peer is a traitor";
    TraitorPeerException exception = new TraitorPeerException(errorMessage);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testTraitorPeerExceptionWithMessageAndCause() {
    String errorMessage = "Peer is a traitor and caused an error";
    Throwable cause = new Throwable("Underlying cause");
    TraitorPeerException exception = new TraitorPeerException(errorMessage, cause);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testTransactionExpirationExceptionDefaultConstructor() {
    TransactionExpirationException exception = new TransactionExpirationException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
  }

  @Test
  public void testTransactionExpirationExceptionWithMessage() {
    String errorMessage = "Transaction has expired";
    TransactionExpirationException exception = new TransactionExpirationException(errorMessage);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  public void testTronRuntimeExceptionDefaultConstructor() {
    TronRuntimeException exception = new TronRuntimeException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testTronRuntimeExceptionWithMessage() {
    String errorMessage = "An error occurred";
    TronRuntimeException exception = new TronRuntimeException(errorMessage);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testTronRuntimeExceptionWithMessageAndCause() {
    String errorMessage = "An error occurred due to a cause";
    Throwable cause = new Throwable("Underlying cause");
    TronRuntimeException exception = new TronRuntimeException(errorMessage, cause);
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testTronRuntimeExceptionWithCause() {
    Throwable cause = new Throwable("Underlying cause without message");
    TronRuntimeException exception = new TronRuntimeException(cause);
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testTypeMismatchNamingException_WithRequiredAndActualTypes() {
    try {
      throw new TypeMismatchNamingException("someName", String.class, Integer.class);
    } catch (TypeMismatchNamingException e) {
      assertEquals("Object of type [class java.lang.Integer] available at store location "
          + "[someName] is not assignable to [java.lang.String]", e.getMessage());
      assertEquals(String.class, e.getRequiredType());
      assertEquals(Integer.class, e.getActualType());
    }
  }

  @Test
  public void testTypeMismatchNamingException_WithExplanation() {
    try {
      throw new TypeMismatchNamingException("Custom explanation");
    } catch (TypeMismatchNamingException e) {
      assertEquals("Custom explanation", e.getMessage());
      assertNull(e.getRequiredType());
      assertNull(e.getActualType());
    }
  }

  @Test
  public void testUnLinkedBlockExceptionDefaultConstructor() {
    UnLinkedBlockException exception = new UnLinkedBlockException();
    assertNotNull("Exception should not be null", exception);
    assertNull(exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testUnLinkedBlockExceptionWithMessage() {
    String testMessage = "This block is not linked";
    UnLinkedBlockException exception = new UnLinkedBlockException(testMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testUnLinkedBlockExceptionWithMessageAndCause() {
    String testMessage = "This block is not linked due to an error";
    Throwable testCause = new Throwable("Cause of the error");
    UnLinkedBlockException exception = new UnLinkedBlockException(testMessage, testCause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
    assertEquals("Cause should match", testCause, exception.getCause());
  }

  @Test
  public void testUnReachBlockExceptionDefaultConstructor() {
    UnReachBlockException exception = new UnReachBlockException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testUnReachBlockExceptionWithMessage() {
    String testMessage = "Block is unreachable";
    UnReachBlockException exception = new UnReachBlockException(testMessage);
    assertNotNull(exception);
    assertEquals(testMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testUnReachBlockExceptionWithMessageAndCause() {
    String testMessage = "Block is unreachable due to an error";
    Throwable testCause = new Throwable("Cause of the error");
    UnReachBlockException exception = new UnReachBlockException(testMessage, testCause);
    assertNotNull(exception);
    assertEquals(testCause, exception.getCause());
  }

  @Test
  public void testValidateScheduleExceptionDefaultConstructor() {
    ValidateScheduleException exception = new ValidateScheduleException();

    assertNotNull(exception);

    assertNull(exception.getMessage());
  }

  @Test
  public void testValidateScheduleExceptionConstructorWithMessage() {
    String testMessage = "Schedule validation failed";

    ValidateScheduleException exception = new ValidateScheduleException(testMessage);

    assertNotNull(exception);

    assertEquals(testMessage, exception.getMessage());
  }

  @Test
  public void testValidateSignatureExceptionDefaultConstructor() {
    ValidateSignatureException exception = new ValidateSignatureException();
    assertNotNull("Exception should not be null", exception);
    assertNull(exception.getMessage());
  }

  @Test
  public void testValidateSignatureExceptionConstructorWithMessage() {
    String testMessage = "Signature validation failed";
    ValidateSignatureException exception = new ValidateSignatureException(testMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
  }

  @Test
  public void testVMIllegalExceptionDefaultConstructor() {
    VMIllegalException exception = new VMIllegalException();
    assertNotNull("Exception should not be null", exception);
    assertNull(exception.getMessage());
  }

  @Test
  public void testVMIllegalExceptionConstructorWithMessage() {
    String testMessage = "VM operation is illegal";
    VMIllegalException exception = new VMIllegalException(testMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
  }

  @Test
  public void testZkProofValidateExceptionWithFirstValidatedTrue() {
    String testMessage = "Zero-knowledge proof validation failed, but first part was validated";
    boolean firstValidated = true;

    ZkProofValidateException exception = new ZkProofValidateException(testMessage, firstValidated);

    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
    assertTrue("firstValidated should be true", exception.isFirstValidated());
  }

  @Test
  public void testZkProofValidateExceptionWithFirstValidatedFalse() {
    String testMessage = "Zero-knowledge proof validation failed, first part not validated";
    boolean firstValidated = false;

    ZkProofValidateException exception = new ZkProofValidateException(testMessage, firstValidated);
    exception.setFirstValidated(true);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
    assertTrue("firstValidated should be true", exception.isFirstValidated());
  }

  @Test
  public void testZksnarkExceptionNoMessage() {
    ZksnarkException exception = new ZksnarkException();
    assertNotNull("Exception should not be null", exception);
    assertNull(exception.getMessage());
  }

  @Test
  public void testZksnarkExceptionWithMessage() {
    String testMessage = "Zksnark validation failed";
    ZksnarkException exception = new ZksnarkException(testMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
  }

  @Test
  public void testTronDBExceptionNoArgs() {
    TronDBException exception = new TronDBException();
    assertNotNull("Exception should not be null", exception);
    assertNull("Message should be null", exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testTronDBExceptionWithMessage() {
    String testMessage = "Database error occurred";
    TronDBException exception = new TronDBException(testMessage);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
    assertNull("Cause should be null", exception.getCause());
  }

  @Test
  public void testTronDBExceptionWithMessageAndThrowable() {
    String testMessage = "Database error with specific cause";
    Throwable testCause = new Throwable("Root cause");
    TronDBException exception = new TronDBException(testMessage, testCause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Message should match", testMessage, exception.getMessage());
    assertEquals("Cause should match", testCause, exception.getCause());
  }

  @Test
  public void testTronDBExceptionWithThrowable() {
    Throwable testCause = new Throwable("Root cause without message");
    TronDBException exception = new TronDBException(testCause);
    assertNotNull("Exception should not be null", exception);
    assertEquals("Cause should match", testCause, exception.getCause());
  }
}
