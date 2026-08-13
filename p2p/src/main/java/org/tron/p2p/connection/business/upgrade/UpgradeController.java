package org.tron.p2p.connection.business.upgrade;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;
import org.tron.p2p.protos.Connect.CompressMessage;

import org.tron.p2p.utils.ProtoUtil;

public class UpgradeController {

  public static byte[] codeSendData(int version, byte[] data) throws IOException {
    if (!supportCompress(version)) {
      return data;
    }
    return ProtoUtil.compressMessage(data).toByteArray();
  }

  public static byte[] decodeReceiveData(int version, byte[] data) throws P2pException, IOException {
    if (!supportCompress(version)) {
      return data;
    }
    CompressMessage compressMessage;
    try {
      compressMessage = CompressMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new P2pException(TypeEnum.PARSE_MESSAGE_FAILED, e);
    }
    return ProtoUtil.uncompressMessage(compressMessage);
  }

  private static boolean supportCompress(int version) {
    return Parameter.version >= 1 && version >= 1;
  }

}
