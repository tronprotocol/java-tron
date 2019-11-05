package org.tron.core.exception;

import javax.naming.NamingException;

@SuppressWarnings("serial")
public class TypeMismatchNamingException extends NamingException {

  private Class<?> requiredType;

  private Class<?> actualType;


  public TypeMismatchNamingException(String name, Class<?> requiredType, Class<?> actualType) {
    super("Object of type [" + actualType + "] available at store location [" +
        name + "] is not assignable to [" + requiredType.getName() + "]");
    this.requiredType = requiredType;
    this.actualType = actualType;
  }

  public TypeMismatchNamingException(String explanation) {
    super(explanation);
  }

  public final Class<?> getRequiredType() {
    return this.requiredType;
  }

  public final Class<?> getActualType() {
    return this.actualType;
  }

}
