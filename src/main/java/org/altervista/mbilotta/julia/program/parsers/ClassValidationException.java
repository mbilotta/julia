package org.altervista.mbilotta.julia.program.parsers;


public class ClassValidationException extends Exception {

  private final String className;
  private final String propertyName;

  public ClassValidationException(Parameter<?>.Validator validator, String message) {
    this(validator, message, null);
  }

  public ClassValidationException(Parameter<?>.Validator validator, String message, Throwable cause) {
    super(message, cause);
    this.className = validator.getPluginType().getName();
    this.propertyName = validator.getParameterId();
  }

  public ClassValidationException(Parameter<?> parameter, String message) {
    this(parameter, message, null);
  }

  public ClassValidationException(Parameter<?> parameter, String message, Throwable cause) {
    super(message, cause);
    this.className = parameter.getType().getName();
    this.propertyName = parameter.getId();
  }

  public String getClassName() {
    return className;
  }

  public String getPropertyName() {
    return propertyName;
  }
}