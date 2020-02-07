package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import org.junit.Assert;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;


public class ActuatorTest {

  private AbstractActuator actuator;
  private Manager dbManager;
  private Any contract = null;
  private Any invalidContract;
  private String expectedcontractTypeMsg;
  private String failMsg;
  private String expectedMsg;
  private String nullChainMangerErrorMsg = "No account store or contract store!";
  private String ownerAddress = null;

  public ActuatorTest(Any contract, AbstractActuator actuator, Manager dbManager) {
    this.actuator = actuator;
    this.dbManager = dbManager;
    this.contract = contract;
  }

  public ActuatorTest(AbstractActuator actuator, Manager dbManager) {
    this.actuator = actuator;
    this.dbManager = dbManager;
  }

  public ActuatorTest(AbstractActuator actuator) {
    this.actuator = actuator;
  }

  public void setMessage(String failMsg, String expectMsg) {
    this.failMsg = failMsg;
    this.expectedMsg = expectMsg;
  }

  public void setInvalidContractTypeMsg(String failMsg, String expectcontractTypeMsg) {
    this.failMsg = failMsg;
    this.expectedcontractTypeMsg = expectcontractTypeMsg;
  }

  public void setContract(Any contract) {
    this.contract = contract;
  }

  public void setDbManager(Manager dbManager) {
    this.dbManager = dbManager;
  }

  public void setInvalidContract(Any invalidContract) {
    this.invalidContract = invalidContract;
  }

  public void setNullDBManagerMsg(String dbManagerMsg) {
    this.nullChainMangerErrorMsg = dbManagerMsg;
  }

  public void setOwnerAddress(String ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public void freeObject() {
    this.contract = null;
    this.invalidContract = null;
    this.actuator = null;
    this.failMsg = null;
    this.expectedcontractTypeMsg = null;
  }


  public void invalidOwnerAddress() {

    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(this.contract);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, this.failMsg,
        this.expectedMsg);
  }

  /**
   * No account store, null DB Manager
   */

  public void nullDBManger() {
    if (this.contract == null) {
      Assert.assertTrue(false);
      return;
    }
    actuator.setChainBaseManager(null)
        .setAny(this.contract);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, nullChainMangerErrorMsg,
        nullChainMangerErrorMsg);
  }

  /**
   * No contract exception test, null contract
   */

  public void noContract() {

    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(null);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  /**
   * invalid contract exception, create PermissionAddKeyContract as an invalid contract
   */
  public void invalidContractType() {
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(this.invalidContract);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "contract type error",
        this.expectedcontractTypeMsg + this.invalidContract.getClass() + "]");
  }

  /**
   * invalid TransactionResultCapsule exception
   */

  public void nullTransationResult() {
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(this.contract);
    TransactionResultCapsule ret = null;
    processAndCheckInvalid(actuator, ret, "TransactionResultCapsule is null",
        "TransactionResultCapsule is null");
  }

  private void processAndCheckInvalid(AbstractActuator actuator,
      TransactionResultCapsule ret,
      String failMsg,
      String expectedMsg) {
    try {
      actuator.validate();
      actuator.execute(ret);
      fail(failMsg);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (RuntimeException e) {
      Assert.assertTrue(e instanceof RuntimeException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    }
  }


}
