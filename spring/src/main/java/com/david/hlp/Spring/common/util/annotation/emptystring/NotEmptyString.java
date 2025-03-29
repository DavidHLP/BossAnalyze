package com.david.hlp.Spring.common.util.annotation.emptystring;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyStringValidator.class)
public @interface NotEmptyString {
    String message() default "不能为空字符串或特殊空值";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
