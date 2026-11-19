package org.tron.core.net.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.tron.common.overlay.message.Message;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.ReasonCode;
import org.tron.protos.Protocol.Transaction;

public class TronMessageFactoryTest {

  @Test
  public void trxIsRejectedBeforeParsingRegardlessOfPayloadContents() {
    byte[][] payloads = {new byte[0], {0x0c}, {0x0a, 0x7f},
        new byte[Parameter.MAX_MESSAGE_LENGTH - 2]};
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(() -> Message.getCodedInputStream(any(byte[].class)))
          .thenThrow(new AssertionError("TRX payload must not be parsed"));
      for (byte[] payload : payloads) {
        P2pException exception = assertThrows(P2pException.class,
            () -> TronMessageFactory.create(withType(MessageTypes.TRX, payload)));
        assertEquals(TypeEnum.NO_SUCH_MESSAGE, exception.getType());
        assertTrue(exception.getMessage().startsWith("TRX, len="));
      }
    }
  }

  @Test
  public void canonicalTrxIsRejectedBeforeMessageConstruction() {
    byte[] payload = sampleTransaction().toByteArray();
    try (MockedConstruction<TransactionMessage> messages =
        mockConstruction(TransactionMessage.class)) {
      P2pException exception = assertThrows(P2pException.class,
          () -> TronMessageFactory.create(withType(MessageTypes.TRX, payload)));
      assertEquals(TypeEnum.NO_SUCH_MESSAGE, exception.getType());
      assertTrue(messages.constructed().isEmpty());
    }
  }

  @Test
  public void trxIsRejectedAtP2pEntryWithNoSuchMessage() {
    Channel channel = mock(Channel.class);
    PeerConnection peer = mock(PeerConnection.class);
    when(peer.getHelloMessageReceive()).thenReturn(mock(HelloMessage.class));
    when(peer.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 10001));
    try (MockedStatic<PeerManager> peers = mockStatic(PeerManager.class)) {
      peers.when(() -> PeerManager.getPeerConnection(channel)).thenReturn(peer);
      new P2pEventHandlerImpl().onMessage(channel,
          withType(MessageTypes.TRX, new byte[] {0x0c}));
      verify(peer).disconnect(ReasonCode.NO_SUCH_MESSAGE);
      verify(peer, never()).getPeerStatistics();
    }
  }

  @Test
  public void directTransactionConstructionRetainsItsExistingBehavior() throws Exception {
    Transaction transaction = sampleTransaction();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      assertEquals(transaction,
          new TransactionMessage(transaction.toByteArray()).getTransactionCapsule().getInstance());
      assertEquals(transaction,
          new TransactionMessage(transaction).getTransactionCapsule().getInstance());
    }
  }

  @Test
  public void supportedMessageTypesStillDecode() throws Exception {
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      TransactionsMessage batch = (TransactionsMessage) TronMessageFactory.create(
          withType(MessageTypes.TRXS, new byte[0]));
      assertEquals(0, batch.getTransactions().getTransactionsCount());
      assertEquals(MessageTypes.P2P_PING, TronMessageFactory.create(
          withType(MessageTypes.P2P_PING, new byte[] {(byte) 0xc0})).getType());
    }
  }

  private static Transaction sampleTransaction() {
    return Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .setTimestamp(123456789L).setExpiration(123516789L)).build();
  }

  private static byte[] withType(MessageTypes type, byte[] payload) {
    byte[] data = new byte[payload.length + 1];
    data[0] = type.asByte();
    System.arraycopy(payload, 0, data, 1, payload.length);
    return data;
  }
}
