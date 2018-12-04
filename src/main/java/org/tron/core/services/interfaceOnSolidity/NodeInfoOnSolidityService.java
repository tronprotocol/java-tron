package org.tron.core.services.interfaceOnSolidity;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.NodeInfo;
import org.tron.common.entity.NodeInfo.ConfigNodeInfo;
import org.tron.common.entity.NodeInfo.MachineInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.DeadLockThreadInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.MemoryDescInfo;
import org.tron.common.entity.PeerInfo;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.core.services.WitnessProductBlockService.CheatWitnessInfo;
import org.tron.program.Version;
import org.tron.protos.Protocol.ReasonCode;

@Component
public class NodeInfoOnSolidityService extends NodeInfoService {

  @Override
  protected void setBlockInfo(NodeInfo nodeInfo) {
//    nodeInfo.setBeginSyncNum(dbManager.getSyncBeginNumber());
    nodeInfo.setBlock(dbManager.getHeadBlockId().getString());
    nodeInfo.setSolidityBlock(dbManager.getHeadBlockId().getString());
  }

  @Override
  protected void setCheatWitnessInfo(NodeInfo nodeInfo) {
  }

}
