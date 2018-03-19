package org.tron.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.tron.common.overlay.message.Message;

public class SafeMessageMap {

  protected final Map<Sha256Hash, Message> storage;

  protected ReadWriteLock rwLock = new ReentrantReadWriteLock();
  protected ALock readLock = new ALock(rwLock.readLock());
  protected ALock writeLock = new ALock(rwLock.writeLock());

  public SafeMessageMap() {
    this.storage = new HashMap<>();
  }

  public void put(Sha256Hash msgId, Message msg) {
    if (msg == null) {
      delete(msgId);
    } else {
      try (ALock l = writeLock.lock()) {
        storage.put(msgId, msg);
      }
    }
  }

  public void put(Message msg) {
    put(Sha256Hash.of(msg.getData()), msg);
  }

  public Message get(Sha256Hash msgId) {
    try (ALock l = readLock.lock()) {
      return storage.get(msgId);
    }
  }

  public void delete(Sha256Hash msgId) {
    try (ALock l = writeLock.lock()) {
      storage.remove(msgId);
    }
  }
}
