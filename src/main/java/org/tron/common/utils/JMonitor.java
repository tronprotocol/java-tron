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

  static Transaction newTransaction(String type, String name) {
    if (isEnabled()) {
      return Cat.newTransaction(type, name);
    }

    return CheatTransaction.TRANSACTION;
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

  interface Session extends Transaction {

  }

  final class CheatTransaction implements Session {

    private static final Transaction TRANSACTION = new CheatTransaction();

    @Override
    public Transaction addChild(Message message) {
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
}
