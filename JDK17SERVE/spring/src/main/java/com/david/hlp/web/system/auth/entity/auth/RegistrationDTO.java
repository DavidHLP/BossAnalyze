package com.david.hlp.web.system.auth.entity.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDTO {

    private String name;

    private String email;

    private String password;

    private String code;
}