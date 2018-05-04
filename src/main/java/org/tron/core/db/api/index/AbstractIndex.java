package org.tron.core.db.api.index;

import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.persistence.Persistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.api.index.Index.Iface;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db.common.WrappedResultSet;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractIndex<E extends ProtoCapsule, T> implements Iface<T> {

  protected TronDatabase<E> database;
  protected ConcurrentIndexedCollection<WrappedByteArray> index;
  private File parent = new File(Args.getInstance().getOutputDirectory() + "index");
  protected File indexPath;
  private ExecutorService service = Executors.newSingleThreadExecutor();

  public AbstractIndex() {
    if (!parent.exists()) {
      parent.mkdirs();
    }
    indexPath = new File(parent, getName() + ".index");
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
      @SuppressWarnings("unchecked")
      T t = (T) database.get(key).getInstance();
      return t;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected T getObject(final WrappedByteArray byteArray) {
    return getObject(byteArray.getBytes());
  }

  protected void fill() {
    int size = Iterables.size(database);
    if (size != 0 && (!indexPath.exists() || index.size() < size)) {
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
    return update(bytes);
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
        return Iterables.transform(resultSet, AbstractIndex.this::getObject).iterator();
      }
    };
  }

  @Override
  public ResultSet<T> retrieve(Query<WrappedByteArray> query, QueryOptions options) {
    ResultSet<WrappedByteArray> resultSet = index.retrieve(query, options);
    return new WrappedResultSet<T>(resultSet) {
      @Override
      public Iterator<T> iterator() {
        return Iterables.transform(resultSet, AbstractIndex.this::getObject).iterator();
      }
    };
  }

  @Override
  public Iterator<T> iterator() {
    return Iterables.transform(index, AbstractIndex.this::getObject).iterator();
  }

  protected abstract void setAttribute();
}
