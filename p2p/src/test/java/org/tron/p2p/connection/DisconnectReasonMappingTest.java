package org.tron.p2p.connection;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.protos.Connect.DisconnectReason;

/**
 * Covers the DisconnectCode -> DisconnectReason mapping. The code is what a peer sends
 * on the wire; the reason is what we record and report. Two of the pairs are deliberately
 * not same-named, so a careless edit here silently mislabels why peers were dropped.
 */
public class DisconnectReasonMappingTest {

  @Test
  public void mapsEachHandshakeCodeToItsReason() {
    Assert.assertEquals(DisconnectReason.DIFFERENT_VERSION,
        ChannelManager.getDisconnectReason(DisconnectCode.DIFFERENT_VERSION));
    Assert.assertEquals(DisconnectReason.DUPLICATE_PEER,
        ChannelManager.getDisconnectReason(DisconnectCode.DUPLICATE_PEER));
    Assert.assertEquals(DisconnectReason.TOO_MANY_PEERS,
        ChannelManager.getDisconnectReason(DisconnectCode.TOO_MANY_PEERS));
  }

  @Test
  public void mapsTheTwoCodesThatChangeName() {
    // TIME_BANNED is reported as RECENT_DISCONNECT
    Assert.assertEquals(DisconnectReason.RECENT_DISCONNECT,
        ChannelManager.getDisconnectReason(DisconnectCode.TIME_BANNED));
    // MAX_CONNECTION_WITH_SAME_IP is reported as TOO_MANY_PEERS_WITH_SAME_IP
    Assert.assertEquals(DisconnectReason.TOO_MANY_PEERS_WITH_SAME_IP,
        ChannelManager.getDisconnectReason(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP));
  }

  @Test
  public void mapsEveryOtherCodeToUnknown() {
    // NORMAL has no disconnect reason of its own, and any code added later must fall
    // through to UNKNOWN rather than to whatever case precedes it
    Assert.assertEquals(DisconnectReason.UNKNOWN,
        ChannelManager.getDisconnectReason(DisconnectCode.NORMAL));
  }

  @Test
  public void mappingIsTotalOverTheEnum() {
    // no code may produce a null reason, since the result is logged unguarded
    for (DisconnectCode code : DisconnectCode.values()) {
      Assert.assertNotNull("no reason mapped for " + code,
          ChannelManager.getDisconnectReason(code));
    }
  }
}
