package org.tron.common.utils;

import java.util.Optional;
import org.tron.core.db.AbstractRevokingStore.Dialog;

public final class DialogOptional {

  private static final DialogOptional INSTANCE = OptionalEnum.INSTANCE.getInstance();

  private Optional<Dialog> value;

  private DialogOptional() {
    this.value = Optional.empty();
  }

  public synchronized DialogOptional setValue(Dialog value) {
    this.value = Optional.ofNullable(value);
    return this;
  }

  public synchronized boolean valid() {
    return value.isPresent();
  }

  public synchronized void reset() {
    value.ifPresent(Dialog::destroy);
    value = Optional.empty();
  }

  public static DialogOptional instance() {
    return INSTANCE;
  }

  private enum OptionalEnum {
    INSTANCE;

    private DialogOptional instance;

    OptionalEnum() {
      instance = new DialogOptional();
    }

    private DialogOptional getInstance() {
      return instance;
    }
  }

}
