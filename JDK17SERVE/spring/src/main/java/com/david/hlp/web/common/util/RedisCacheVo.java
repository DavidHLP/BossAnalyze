package com.david.hlp.web.common.util;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisCacheVo<T> implements Serializable {
    private T data;
    private LocalDateTime cacheExpireTime;
}
