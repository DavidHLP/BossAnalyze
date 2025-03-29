package com.david.hlp.Spring.system.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private Long id;
    private Long userId;
    private String token;
    private TokenType tokenType;
    private boolean expired;
    private boolean revoked;
}