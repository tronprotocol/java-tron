package org.tron.core.net.message.adv;

import static org.tron.core.Constant.MIN_TRANSACTION_SIZE;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import java.io.IOException;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol.Block;

public class BlockMessage extends TronMessage {

  public static final int MAX_BLOCK_SIZE = BLOCK_SIZE + Constant.ONE_THOUSAND;

  // Every accepted transaction is at least MIN_TRANSACTION_SIZE bytes; header and
  // repeated-field overhead only make this upper bound more conservative.
  private static final int MAX_TRANSACTION_COUNT = MAX_BLOCK_SIZE / MIN_TRANSACTION_SIZE;
  private static final int TRANSACTIONS_TAG = (Block.TRANSACTIONS_FIELD_NUMBER << 3)
      | WireFormat.WIRETYPE_LENGTH_DELIMITED;

  private BlockCapsule block;

  public BlockMessage(byte[] data) throws Exception {
    super(data);
    precheck(data);
    this.type = MessageTypes.BLOCK.asByte();
    this.block = new BlockCapsule(getCodedInputStream(data));
    if (Message.isFilter()) {
      Message.compareBytes(data, block.getInstance().toByteArray());
      TransactionCapsule.validContractProto(block.getInstance().getTransactionsList());
    }
  }

  private void precheck(byte[] data) throws IOException, P2pException {
    if (data.length > MAX_BLOCK_SIZE) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
    }
    CodedInputStream input = CodedInputStream.newInstance(data);
    int transactionCount = 0;
    int tag;
    while ((tag = input.readTag()) != 0) {
      if (tag == TRANSACTIONS_TAG && ++transactionCount > MAX_TRANSACTION_COUNT) {
        throw new P2pException(TypeEnum.BAD_MESSAGE, "block transaction count over limit");
      }
      // Skip each field without constructing transactions, bytes or unknown-field objects.
      if (!input.skipField(tag)) {
        break;
      }
    }
    // A top-level block must end at EOF, not at an unmatched END_GROUP tag.
    input.checkLastTagWas(0);
  }

  public BlockMessage(BlockCapsule block) {
    data = block.getData();
    this.type = MessageTypes.BLOCK.asByte();
    this.block = block;
  }

  public void sanitize() {
    if (this.block.sanitize()) {
      this.data = this.block.getData();
    }
  }

  public BlockId getBlockId() {
    return getBlockCapsule().getBlockId();
  }

  public BlockCapsule getBlockCapsule() {
    return block;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public Sha256Hash getMessageId() {
    return getBlockCapsule().getBlockId();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(block.getBlockId().getString())
        .append(", trx size: ").append(block.getTransactions().size()).append("\n").toString();
  }
}
