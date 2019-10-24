package org.tron.core.spv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.SyncPool;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.spv.message.BlockHeaderMessage;
import org.tron.core.spv.message.DownloadHeaderMessage;
import org.tron.core.spv.message.NotDataDownloadMessage;
import org.tron.protos.Protocol.DownloadHeader;
import org.tron.protos.Protocol.Items;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
public class HeaderMsgAction {

  private static final long DOWNLOAD_COUNT = 200L;

  @Autowired
  private Manager manager;
  @Autowired
  private BlockStore blockStore;
  @Autowired
  private PbftSignDataStore pbftSignDataStore;
  @Autowired
  private HeaderManager headerManager;
  @Autowired
  private SyncPool syncPool;

  private Cache<String, Boolean> uuidCache = CacheBuilder.newBuilder().initialCapacity(100)
      .maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  public void processDownloadBlockHeaderMsg(PeerConnection peer, TronMessage msg) {
    DownloadHeaderMessage downloadHeaderMessage = (DownloadHeaderMessage) msg;
    try {
      BlockId headBlockId = downloadHeaderMessage.getHeadBlockId();
      BlockId solidityBlockId = downloadHeaderMessage.getSolidityBlockId();
      if (solidityBlockId.getNum() == 0) {
        download(peer, 0, downloadHeaderMessage);
        return;
      }
      if (!manager.containBlock(solidityBlockId)) {
        peer.disconnect(ReasonCode.FETCH_FAIL);
        return;
      }
      if (!manager.containBlockInMainChain(headBlockId)) {
        //begin sync from solidity high
        download(peer, solidityBlockId.getNum() + 1, downloadHeaderMessage);
        return;
      }
      //begin sync from head high
      download(peer, headBlockId.getNum() + 1, downloadHeaderMessage);
    } catch (HeaderNotFound headerNotFound) {
      peer.disconnect(ReasonCode.UNRECOGNIZED);
    }
  }

  private void download(PeerConnection peer, long begin,
      DownloadHeaderMessage downloadHeaderMessage) throws HeaderNotFound {
    BlockCapsule localHead = manager.getHead();
    long downloadCount = DOWNLOAD_COUNT;
    if (localHead.getNum() - begin < DOWNLOAD_COUNT) {
      downloadCount = localHead.getNum() - begin + 1;
    }
    if (downloadCount == 0) {
      peer.sendMessage(new NotDataDownloadMessage(downloadHeaderMessage.getUuid()));
      return;
    }
    List<BlockCapsule> blockCapsuleList = blockStore.getLimitNumber(begin, downloadCount);
    for (BlockCapsule blockCapsule : blockCapsuleList) {
      blockCapsule.cleanTransactions(pbftSignDataStore.getBlockSignData(blockCapsule.getNum()));
    }
    peer.sendMessage(new BlockHeaderMessage(blockCapsuleList, downloadHeaderMessage.getUuid()));
  }

  public void processBlockHeadersMsg(PeerConnection peer, TronMessage msg) throws P2pException {
    BlockHeaderMessage blockHeaderMessage = (BlockHeaderMessage) msg;
    validMsg(blockHeaderMessage.getItems());
  }

  public void processNotDataDownload(PeerConnection peer, TronMessage msg) throws P2pException {
    NotDataDownloadMessage notDataDownloadMessage = (NotDataDownloadMessage) msg;
    validMsg(notDataDownloadMessage.getItems());
  }

  public void startDownloadHeader(String chainId) throws ItemNotFoundException {
    BlockId solidityBlockId = headerManager.getSolidBlockId(chainId);
    DownloadHeaderMessage downloadHeaderMessage = null;
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    if (solidityBlockId == null) {
      DownloadHeader.Builder downloadHeader = DownloadHeader.newBuilder();
      downloadHeader.setUuid(ByteString.copyFromUtf8(uuid));
      downloadHeaderMessage = new DownloadHeaderMessage(downloadHeader.build());
    } else {
      BlockId headBlockId = headerManager.getHead(chainId);
      DownloadHeader.Builder downloadHeader = DownloadHeader.newBuilder();
      DownloadHeader.BlockId head = DownloadHeader.BlockId.newBuilder()
          .setHash(headBlockId.getByteString()).setNumber(headBlockId.getNum()).build();
      DownloadHeader.BlockId solidity = DownloadHeader.BlockId.newBuilder()
          .setHash(solidityBlockId.getByteString()).setNumber(solidityBlockId.getNum()).build();
      downloadHeader.setHeaderId(head).setSolidityId(solidity)
          .setUuid(ByteString.copyFromUtf8(uuid));
      downloadHeaderMessage = new DownloadHeaderMessage(downloadHeader.build());
    }
    PeerConnection peerConnection = selectPeer(chainId);
    if (peerConnection != null) {
      uuidCache.put(uuid, true);
      peerConnection.sendMessage(downloadHeaderMessage);
    }
  }

  private PeerConnection selectPeer(String chainId) {
    List<PeerConnection> peerConnectionList = syncPool.getActivePeers();
    return peerConnectionList.get(0);
  }

  private void validMsg(Items items) throws P2pException {
    if (uuidCache.getIfPresent(items.getUuid().toStringUtf8()) == null) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "not me request msg");
    }
  }
}
