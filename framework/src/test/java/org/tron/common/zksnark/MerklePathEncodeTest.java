package org.tron.common.zksnark;

import static org.junit.Assert.assertThrows;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.tron.core.exception.ZksnarkException;

public class MerklePathEncodeTest {

  @Test
  public void testEncode_sizeMismatch_throwsZksnarkException() {
    List<List<Boolean>> authPath = Arrays.asList(
        Arrays.asList(true, false),
        Arrays.asList(false, true)
    );
    List<Boolean> index = Collections.singletonList(true); // size 1, authPath size 2

    MerklePath merklePath = new MerklePath(authPath, index);

    assertThrows(ZksnarkException.class, merklePath::encode);
  }

  @Test
  public void testEncode_emptyLists_noException() throws ZksnarkException {
    MerklePath merklePath = new MerklePath(Lists.newArrayList(), Lists.newArrayList());
    merklePath.encode(); // should not throw
  }
}
