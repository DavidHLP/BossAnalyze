package com.david.hlp.web.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultBody<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
}