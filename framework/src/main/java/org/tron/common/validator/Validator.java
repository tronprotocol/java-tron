package org.tron.common.validator;

public interface Validator<V> {

  String validate(final V v);

  Validator<V> nextValidator(Validator<V> next);
}
