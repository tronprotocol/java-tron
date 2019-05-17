package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.Protocol.TransactionResultList;

@Slf4j(topic = "capsule")
public class TransactionResultListCapsule implements ProtoCapsule<TransactionResultList> {
  private TransactionResultList transactionResultList;

  public TransactionResultListCapsule(BlockCapsule blockCapsule) {
    transactionResultList = TransactionResultList.newBuilder().build();
    if (Objects.isNull(blockCapsule)) {
      return;
    }
    TransactionResultList.Builder build = transactionResultList.toBuilder().
        setBlockNumber(blockCapsule.getNum()).setBlockTimeStamp(blockCapsule.getTimeStamp());
    transactionResultList = build.build();
  }

  public void addTransactionResult(TransactionInfo result) {
    this.transactionResultList = this.transactionResultList.toBuilder().addTransactioninfo(result).build();
  }

  @Override
  public byte[] getData() {
    return transactionResultList.toByteArray();
  }

  @Override
  public TransactionResultList getInstance() {
    return transactionResultList;
  }
}