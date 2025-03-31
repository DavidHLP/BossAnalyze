package com.david.hlp.Spring.crawler.proxy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthProxyInfo {
    private final String ip;
    private final int port;
    private final String username;
    private final String password;
    @Builder.Default
    private boolean blocked = false;
}
