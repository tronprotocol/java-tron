package org.tron.core.metrics.node;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.program.Version;


@Component
public class NodeMetricManager {

  @Autowired
  ChainBaseManager chainBaseManager;

  @Autowired
  private BackupManager backupManager;


  /**
   * get node info.
   *
   * @return NodeInfo
   */
  public NodeInfo getNodeInfo() {
    NodeInfo nodeInfo = new NodeInfo();
    setNodeInfo(nodeInfo);
    return nodeInfo;
  }

  private void setNodeInfo(NodeInfo nodeInfo) {

    nodeInfo.setIp(Args.getInstance().getNodeExternalIp());

    ByteString witnessAddress = ByteString.copyFrom(Args.getLocalWitnesses()
        .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine()));
    if (chainBaseManager.getWitnessScheduleStore().getActiveWitnesses().contains(witnessAddress)) {
      nodeInfo.setNodeType(1);
    } else {
      nodeInfo.setNodeType(0);
    }

    nodeInfo.setVersion(Version.getVersion());
    if (backupManager.getStatus() == BackupManager.BackupStatusEnum.MASTER) {
      nodeInfo.setBackupStatus(1);
    } else {
      nodeInfo.setBackupStatus(0);
    }
  }


}
