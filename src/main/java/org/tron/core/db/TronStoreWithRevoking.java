package org.tron.core.db;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.IndexHelper;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.db2.core.RevokingDBWithCachingNewValue;
import org.tron.core.db2.core.RevokingDBWithCachingOldValue;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> implements ITronChainBase<T> {
  @Getter // only for unit test
  protected IRevokingDB revokingDB;
  private TypeToken<T> token = new TypeToken<T>(getClass()) {};
  @Autowired
  private RevokingDatabase revokingDatabase;
  @Autowired(required = false)
  protected IndexHelper indexHelper;
  @Getter
  private String dbName;

  protected TronStoreWithRevoking(String dbName) {
    this.dbName = dbName;
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (dbVersion == 1) {
      this.revokingDB = new RevokingDBWithCachingOldValue(dbName);
    } else if (dbVersion == 2) {
      this.revokingDB = new RevokingDBWithCachingNewValue(dbName);
    } else {
      throw new RuntimeException("db version is error.");
    }
  }

  @PostConstruct
  private void init() {
    revokingDatabase.add(revokingDB);
  }

  // only for test
  protected TronStoreWithRevoking(String dbName, RevokingDatabase revokingDatabase) {
      this.revokingDB = new RevokingDBWithCachingOldValue(dbName, (AbstractRevokingStore) revokingDatabase);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }

    revokingDB.put(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    return of(revokingDB.get(key));
  }

  @Override
  public T getUnchecked(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);

    try {
      return of(value);
    } catch (BadItemException e) {
      return null;
    }
  }

  public T getOnSolidity(byte[] key) throws ItemNotFoundException, BadItemException {
    return of(revokingDB.getOnSolidity(key));
  }

  public T getUncheckedOnSolidity(byte[] key) {
    byte[] value = revokingDB.getUncheckedOnSolidity(key);

    try {
      return of(value);
    } catch (BadItemException e) {
      return null;
    }
  }

  public T of(byte[] value) throws BadItemException {
    try {
      Constructor constructor = token.getRawType().getConstructor(byte[].class);
      @SuppressWarnings("unchecked")
      T t = (T) constructor.newInstance((Object) value);
      return t;
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new BadItemException(e.getMessage());
    }
  }

  public boolean hasOnSolidity(byte[] key) {
    return revokingDB.hasOnSolidity(key);
  }

  @Override
  public boolean has(byte[] key) {
    return revokingDB.has(key);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void close() {
    revokingDB.close();
  }

  @Override
  public void reset() {
    revokingDB.reset();
  }

  @Override
  public Iterator<Map.Entry<byte[], T>> iterator() {
    return Iterators.transform(revokingDB.iterator(), e -> {
      try {
        return Maps.immutableEntry(e.getKey(), of(e.getValue()));
      } catch (BadItemException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  public Iterator<Map.Entry<byte[], T>> iteratorOnSolidity() {
    return Iterators.transform(((RevokingDBWithCachingNewValue) revokingDB).iteratorOnSolidity(), e ->
    {
      try {
        return Maps.immutableEntry(e.getKey(), of(e.getValue()));
      } catch (BadItemException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  public long size() {
    return Streams.stream(revokingDB.iterator()).count();
  }

}
