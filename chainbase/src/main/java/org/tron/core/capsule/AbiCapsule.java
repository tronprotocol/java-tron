package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;


@Slf4j(topic = "capsule")
public class AbiCapsule implements ProtoCapsule<ABI> {

  private ABI abi;

  public AbiCapsule(byte[] data) {
    try {
      this.abi = ABI.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public AbiCapsule(ContractCapsule contract) {
    this.abi = contract.getInstance().getAbi().toBuilder().build();
  }

  public AbiCapsule(ABI abi) {
    this.abi = abi.toBuilder().build();
  }

  @Override
  public byte[] getData() {
    return this.abi.toByteArray();
  }

  @Override
  public ABI getInstance() {
    return this.abi;
  }

  @Override
  public String toString() {
    return this.abi.toString();
  }
}
