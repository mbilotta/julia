package org.altervista.mbilotta.julia;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Repeatable(Authors.class)
@Retention(RUNTIME)
@Target(TYPE)
public @interface Author {

	String name();
	String contact();

}
