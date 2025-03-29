package com.david.hlp.Spring.common.util.annotation.emptystring;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotEmptyStringValidator implements ConstraintValidator<NotEmptyString, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 允许为null，但不允许为空字符串或特殊空值
        }
        return !value.isEmpty() && !"null".equals(value) && !"undefined".equals(value);
    }
}