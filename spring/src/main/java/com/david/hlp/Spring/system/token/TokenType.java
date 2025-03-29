package com.david.hlp.Spring.system.token;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum TokenType implements IEnum<String> {
    BEARER("BEARER"),
    ACCESS("ACCESS"),
    REFRESH("REFRESH"),
    PASS("PASS");
    @EnumValue
    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}