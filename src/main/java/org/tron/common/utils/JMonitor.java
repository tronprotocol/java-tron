package org.tron.common.utils;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import java.util.Collections;
import java.util.List;

public interface JMonitor {

  static void countAndDuration(String key, long value) {
    if (isEnabled()) {
      logMetricForCount(key + ".count");
      logMetricForDuration(key + ".meantime", value);
    }
  }

  static Session newSession(String type, String name) {
    if (isEnabled()) {
      return new DefaultSession(type, name);
    }

    return CheatSession.SESSION;
  }

  static void logEvent(String type, String name) {
    if (isEnabled()) {
      Cat.logEvent(type, name);
    }
  }

  static void logMetricForCount(String name) {
    if (isEnabled()) {
      Cat.logMetricForCount(name);
    }
  }

  static void logMetricForCount(String name, int quantity) {
    if (isEnabled()) {
      Cat.logMetricForCount(name, quantity);
    }
  }

  static void logMetricForDuration(String name, long durationInMillis) {
    if (isEnabled()) {
      Cat.logMetricForDuration(name, durationInMillis);
    }
  }

  static void logMetricForSum(String name, double value) {
    if (isEnabled()) {
      Cat.logMetricForSum(name, value);
    }
  }

  static void logMetricForSum(String name, double sum, int quantity) {
    if (isEnabled()) {
      Cat.logMetricForSum(name, sum, quantity);
    }
  }

  static boolean isEnabled() {
    return true;
  }

  interface Session extends Message {

    Session addChild(Message var1);

    List<Message> getChildren();

    long getDurationInMicros();

    long getDurationInMillis();

    boolean hasChildren();

    boolean isStandalone();

  }

  final class DefaultSession implements Session {

    private Transaction transaction;

    public DefaultSession(String type, String name) {
      transaction = Cat.newTransaction(type, name);
    }

    @Override
    public Session addChild(Message var1) {
      transaction.addChild(var1);
      return this;
    }

    @Override
    public List<Message> getChildren() {
      return transaction.getChildren();
    }

    @Override
    public long getDurationInMicros() {
      return transaction.getDurationInMicros();
    }

    @Override
    public long getDurationInMillis() {
      return transaction.getDurationInMillis();
    }

    @Override
    public boolean hasChildren() {
      return transaction.hasChildren();
    }

    @Override
    public boolean isStandalone() {
      return transaction.isStandalone();
    }

    @Override
    public void addData(String s) {
      transaction.addData(s);
    }

    @Override
    public void addData(String s, Object o) {
      transaction.addData(s, o);
    }

    @Override
    public void complete() {
      transaction.complete();
    }

    @Override
    public Object getData() {
      return transaction.getData();
    }

    @Override
    public String getName() {
      return transaction.getName();
    }

    @Override
    public String getStatus() {
      return transaction.getStatus();
    }

    @Override
    public long getTimestamp() {
      return transaction.getTimestamp();
    }

    @Override
    public String getType() {
      return transaction.getType();
    }

    @Override
    public boolean isCompleted() {
      return transaction.isCompleted();
    }

    @Override
    public boolean isSuccess() {
      return transaction.isSuccess();
    }

    @Override
    public void setStatus(String s) {
      transaction.setStatus(s);
    }

    @Override
    public void setStatus(Throwable throwable) {
      transaction.setStatus(throwable);
    }
  }

  final class CheatSession implements Session {

    private static final Session SESSION = new CheatSession();

    @Override
    public Session addChild(Message message) {
      return this;
    }

    @Override
    public List<Message> getChildren() {
      return Collections.emptyList();
    }

    @Override
    public long getDurationInMicros() {
      return 0;
    }

    @Override
    public long getDurationInMillis() {
      return 0;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public boolean isStandalone() {
      return false;
    }

    @Override
    public void addData(String s) {
    }

    @Override
    public void addData(String s, Object o) {
    }

    @Override
    public void complete() {
    }

    @Override
    public Object getData() {
      return null;
    }

    @Override
    public String getName() {
      return "";
    }

    @Override
    public String getStatus() {
      return "";
    }

    @Override
    public long getTimestamp() {
      return 0;
    }

    @Override
    public String getType() {
      return "";
    }

    @Override
    public boolean isCompleted() {
      return false;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public void setStatus(String s) {
    }

    @Override
    public void setStatus(Throwable throwable) {
    }
  }

  interface Event extends Message {

  }
}
