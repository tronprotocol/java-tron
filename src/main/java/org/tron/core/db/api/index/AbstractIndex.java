package org.tron.core.db.api.index;

import com.google.common.collect.Iterables;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.persistence.Persistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;
import java.io.File;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.index.Index.Iface;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db.common.WrappedResultSet;
import org.tron.core.db2.core.ITronChainBase;

public abstract class AbstractIndex<E extends ProtoCapsule<T>, T> implements Iface<T> {

  protected ITronChainBase<E> database;
  protected ConcurrentIndexedCollection<WrappedByteArray> index;
  private File parent = new File(Args.getInstance().getOutputDirectory() + "index");
  protected File indexPath;

  public AbstractIndex() {
    if (!parent.exists()) {
      parent.mkdirs();
    }
    indexPath = new File(parent, getName() + ".index");
    setAttribute();
  }

  public AbstractIndex(ITronChainBase<E> database) {
    this.database = database;
    String dbName = database.getDbName();
    File parentDir = Paths.get(
        Args.getInstance().getOutputDirectoryByDbName(dbName),
        Args.getInstance().getStorage().getIndexDirectory()
    ).toFile();
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }
    indexPath = new File(parentDir, getName() + ".index");
    setAttribute();
  }

  public void initIndex(Persistence<WrappedByteArray, ? extends Comparable> persistence) {
    index = new ConcurrentIndexedCollection<>(persistence);
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  protected T getObject(final byte[] key) {
    try {
      E e = database.get(key);
      if (Objects.isNull(e)) {
        return null;
      }
      return e.getInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected T getObject(final WrappedByteArray byteArray) {
    return getObject(byteArray.getBytes());
  }

  @Override
  public void fill() {
    int size = Iterables.size(database);
    if (size != 0 && !indexPath.exists()) {
      database.forEach(e -> add(e.getKey()));
    }
  }

  @Override
  public boolean add(byte[] bytes) {
    return add(WrappedByteArray.of(bytes));
  }

  @Override
  public synchronized boolean add(WrappedByteArray bytes) {
    return index.add(bytes);
  }

  @Override
  public boolean update(WrappedByteArray bytes) {
    return add(bytes);
  }

  @Override
  public boolean update(byte[] bytes) {
    return add(WrappedByteArray.of(bytes));
  }

  @Override
  public boolean remove(byte[] bytes) {
    return remove(WrappedByteArray.of(bytes));
  }

  @Override
  public boolean remove(WrappedByteArray bytes) {
    return index.remove(bytes);
  }

  @Override
  public long size() {
    return index.size();
  }

  @Override
  public ResultSet<T> retrieve(Query<WrappedByteArray> query) {
    ResultSet<WrappedByteArray> resultSet = index.retrieve(query);
    return new WrappedResultSet<T>(resultSet) {
      @Override
      public Iterator<T> iterator() {
        return Iterables
            .filter(Iterables.transform(resultSet, AbstractIndex.this::getObject), Objects::nonNull)
            .iterator();
      }
    };
  }

  @Override
  public ResultSet<T> retrieve(Query<WrappedByteArray> query, QueryOptions options) {
    ResultSet<WrappedByteArray> resultSet = index.retrieve(query, options);
    return new WrappedResultSet<T>(resultSet) {
      @Override
      public Iterator<T> iterator() {
        return Iterables
            .filter(Iterables.transform(resultSet, AbstractIndex.this::getObject), Objects::nonNull)
            .iterator();
      }
    };
  }

  @Override
  public Iterator<T> iterator() {
    return Iterables
        .filter(Iterables.transform(index, AbstractIndex.this::getObject), Objects::nonNull)
        .iterator();
  }

  protected abstract void setAttribute();
}
