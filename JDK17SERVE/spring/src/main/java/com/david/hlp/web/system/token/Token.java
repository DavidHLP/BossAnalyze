package com.david.hlp.web.system.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.david.hlp.web.system.entity.auth.AuthUser;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token implements Serializable {
    private Long id;
    private AuthUser authUser;
    private Long userId;
    private String token;
    private TokenType tokenType;
    private boolean expired;
    private boolean revoked;
}