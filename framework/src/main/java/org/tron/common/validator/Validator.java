package org.tron.common.validator;

public interface Validator<T, V> {

  T validate(final V v);

  Validator<T, V> nextValidator(Validator<T, V> next);
}
