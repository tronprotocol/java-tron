package org.tron.core.exception;

import lombok.Getter;
import lombok.Setter;

public class ZkProofValidateException extends ContractValidateException {

  @Getter
  @Setter
  private boolean firstValidated;

  public ZkProofValidateException(String message, boolean firstValidated) {
    super(message);
    this.firstValidated = firstValidated;
  }
}
