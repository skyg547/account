package com.ho.account.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {
    String pattern() default "DEFAULT"; // e.g., "ACCOUNT", "REG_NO", "EMAIL"
}
