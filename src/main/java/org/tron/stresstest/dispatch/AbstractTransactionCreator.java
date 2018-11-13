package org.tron.stresstest.dispatch;

import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.Getter;
import org.tron.core.Wallet;
import org.tron.stresstest.dispatch.strategy.Level2Strategy;

@Getter
public abstract class AbstractTransactionCreator extends Level2Strategy {
  protected String privateKey = "cbe57d98134c118ed0d219c0c8bc4154372c02c1e13b5cce30dd22ecd7bed19e";
  protected String witnessPrivateKey = "369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77";
  protected ByteString ownerAddress = ByteString.copyFrom(Wallet.decodeFromBase58Check("27meR2d4HodFPYX2V8YRDrLuFpYdbLvBEWi"));
  protected ByteString witnessAddress = ByteString.copyFrom(Wallet.decodeFromBase58Check("27QAUYjg5FXfxcvyHcWF3Aokd5eu9iYgs1c"));
  protected ByteString toAddress = ByteString.copyFrom(Wallet.decodeFromBase58Check("27ZESitosJfKouTBrGg6Nk5yEjnJHXMbkZp"));
  protected Long amount = 1L;
  protected Long amountOneTrx = 1000_000L;
  protected ByteString assetName = ByteString.copyFrom("pressure1", Charsets.UTF_8);

  // deploy contract
  protected String contractName = "createContract";
  protected String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"int256\"},{\"name\":\"b\",\"type\":\"int256\"}],\"name\":\"multiply\",\"outputs\":[{\"name\":\"output\",\"type\":\"int256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"a\",\"type\":\"int256\"},{\"indexed\":false,\"name\":\"b\",\"type\":\"int256\"},{\"indexed\":false,\"name\":\"output\",\"type\":\"int256\"}],\"name\":\"MultiplyEvent\",\"type\":\"event\"}]";
  protected String code = "6080604052348015600f57600080fd5b5060e98061001e6000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633c4308a881146043575b600080fd5b348015604e57600080fd5b50605b600435602435606d565b60408051918252519081900360200190f35b60408051338152602081018490528082018390528383026060820181905291517feb4e4c25ee4bb2b9466eb38f13989c0c221efa6f1c631b8b4820f00afcf5a3e59181900360800190a1929150505600a165627a7a723058200dbf85f2b87350cd0aaa578b300b50d62fb3508880a151d2db70356c1fe463da0029";
  protected String data = null;
  protected long value = 0;
  protected long consumeUserResourcePercent = 100;
  protected String libraryAddress = null;

  // trigger contract
  protected byte[] contractAddress = Wallet.decodeFromBase58Check("27UscVhqkUcCmZzzG1UQthRdiAtY4X4LiUD"); // 通过wallet-cli部署合约后得到合约地址

  protected HashMap<String, String> voteWitnessMap = new HashMap<String, String>() {
    {
      put("27QAUYjg5FXfxcvyHcWF3Aokd5eu9iYgs1c", "1");
      put("27g8BKC65R7qsnEr2vf7R2Nm7MQfvuJ7im4", "1");
      put("27Uoo1DVmYT1fFYPdnTtueyqHkjR3DaDjwo", "1");
      put("27mEGtrpetip67KuXHFShryhGWd8nbSfLRW", "1");
      put("27jvZ4iJ7LQ8UP3VKPGQLp3oj7c7jFf6Q32", "1");
    }
  };
}
