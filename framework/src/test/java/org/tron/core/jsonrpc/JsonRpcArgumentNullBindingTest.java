package org.tron.core.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.core.services.jsonrpc.types.CallArguments;

public class JsonRpcArgumentNullBindingTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testBuildArgumentsExplicitNullMatchesOmittedDefaults() throws Exception {
    BuildArguments omitted = MAPPER.readValue("{}", BuildArguments.class);
    String[] optionalFields = {
        "tokenId",
        "tokenValue",
        "consumeUserResourcePercent",
        "originEnergyLimit",
        "permissionId",
        "extraData"
    };

    for (String field : optionalFields) {
      ObjectNode input = MAPPER.createObjectNode();
      input.putNull(field);
      BuildArguments explicitNull = MAPPER.treeToValue(input, BuildArguments.class);

      assertBuildDefaultsEqual(omitted, explicitNull);
    }
  }

  @Test
  public void testCallArgumentsNullFromMatchesOmittedDefault() throws Exception {
    CallArguments omitted = MAPPER.readValue("{}", CallArguments.class);
    CallArguments explicitNull = MAPPER.readValue("{\"from\":null}", CallArguments.class);

    Assert.assertEquals(omitted.getFrom(), explicitNull.getFrom());
    Assert.assertEquals(
        "0x0000000000000000000000000000000000000000", explicitNull.getFrom());
  }

  private static void assertBuildDefaultsEqual(BuildArguments expected, BuildArguments actual) {
    Assert.assertEquals(expected.getTokenId(), actual.getTokenId());
    Assert.assertEquals(expected.getTokenValue(), actual.getTokenValue());
    Assert.assertEquals(expected.getConsumeUserResourcePercent(),
        actual.getConsumeUserResourcePercent());
    Assert.assertEquals(expected.getOriginEnergyLimit(), actual.getOriginEnergyLimit());
    Assert.assertEquals(expected.getPermissionId(), actual.getPermissionId());
    Assert.assertEquals(expected.getExtraData(), actual.getExtraData());
  }
}
