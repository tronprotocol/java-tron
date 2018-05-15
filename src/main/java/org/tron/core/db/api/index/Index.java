package org.tron.core.db.api.index;

import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;
import org.tron.core.db.common.WrappedByteArray;

public class Index {

  public interface Iface<T> extends Iterable<T> {
    ResultSet<T> retrieve(Query<WrappedByteArray> query);

    ResultSet<T> retrieve(Query<WrappedByteArray> query, QueryOptions options);

    boolean add(byte[] bytes);

    boolean add(WrappedByteArray bytes);

    boolean update(byte[] bytes);

    boolean update(WrappedByteArray bytes);

    boolean remove(byte[] bytes);

    boolean remove(WrappedByteArray bytes);

    long size();

    String getName();

    void fill();
  }
}
