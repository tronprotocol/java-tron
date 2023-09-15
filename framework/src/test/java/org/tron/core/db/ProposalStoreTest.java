package org.tron.core.db;

import static org.junit.Assert.assertThrows;

import com.google.protobuf.ByteString;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.ProposalStore;
import org.tron.protos.Protocol;

public class ProposalStoreTest extends BaseTest {

  @Resource
  private ProposalStore proposalStore;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }

  @Before
  public void init(){
    Protocol.Proposal.Builder builder = Protocol.Proposal.newBuilder()
        .setProposalId(1L).putParameters(1,99).setState(Protocol.Proposal.State.PENDING)
        .setProposerAddress(ByteString.copyFromUtf8("Address1"));
    proposalStore.put("1".getBytes(), new ProposalCapsule(builder.build()));
    builder.setProposalId(2L).setState(Protocol.Proposal.State.APPROVED).setProposerAddress(
        ByteString.copyFromUtf8("Address2"));
    proposalStore.put("2".getBytes(), new ProposalCapsule(builder.build()));
  }

  @Test
  public void testGet() throws Exception {
    final ProposalCapsule result = proposalStore.get("1".getBytes());
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getID(),1);
    Assert.assertEquals(result.getProposalAddress(), ByteString.copyFromUtf8("Address1"));
    assertThrows(ItemNotFoundException.class,
        () -> proposalStore.get("testGet1".getBytes()));
  }

  @Test
  public void testGetAllProposals() {
    final List<ProposalCapsule> result = proposalStore.getAllProposals();
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get(0).getID(), 1);
    Assert.assertEquals(result.get(0).getProposalAddress(), ByteString.copyFromUtf8("Address1"));
  }

  @Test
  public void testGetSpecifiedProposals() {
    final List<ProposalCapsule> result =
        proposalStore.getSpecifiedProposals(Protocol.Proposal.State.PENDING, 1);
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getID(), 1);
    Assert.assertEquals(result.get(0).getProposalAddress(), ByteString.copyFromUtf8("Address1"));
  }
}
