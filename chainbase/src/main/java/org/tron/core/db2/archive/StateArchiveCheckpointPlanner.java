package org.tron.core.db2.archive;

import java.io.IOException;
import java.util.List;
import org.tron.core.db2.core.CommonCheckpointCapture;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointTarget;

/** Computes the transient Archive binding embedded in a Common checkpoint v2 payload. */
public interface StateArchiveCheckpointPlanner extends CommonCheckpointMaterializer {

  StateArchiveHotBatchDescriptor planCheckpoint(List<BlockReverseDiff> diffs)
      throws IOException;

  CommonCheckpointTarget prepare(CommonCheckpointCapture capture) throws IOException;
}
