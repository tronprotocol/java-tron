package org.tron.command;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface CliCommand {
    String[] commands();

    String description() default "";

    boolean needInjection() default false;
}
