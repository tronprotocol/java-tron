package org.tron.common.utils;

import org.tron.core.db.AbstractRevokingStore.Dialog;

public class DialogOptional<T extends Dialog> {
  private static final DialogOptional<?> EMPTY = new DialogOptional<>();

  private java.util.Optional<T> value;

  private DialogOptional() {
    this.value = java.util.Optional.empty();
  }

  public static <T extends Dialog> DialogOptional<T> empty() {
    @SuppressWarnings("unchecked")
    DialogOptional<T> t = (DialogOptional<T>) EMPTY;
    return t;
  }

  private DialogOptional(T value) {
    this.value = java.util.Optional.of(value);
  }

  public static <T extends Dialog> DialogOptional<T> of(T value) {
    return new DialogOptional<>(value);
  }

  public static <T extends Dialog> DialogOptional<T> ofNullable(T value) {
    return value == null ? empty() : of(value);
  }

  public boolean valid() {
    return value.isPresent();
  }

  public void reset() {
    value.ifPresent(Dialog::destroy);
    value = java.util.Optional.empty();
  }
}
