package com.mes.domain.auth.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private String username;
    private String displayName;
    private List<String> roles;
    private boolean authenticated;
    private String accessToken;
    private String tokenType;
    private long expiresIn;

    public static AuthUserResponse anonymous() {
        return new AuthUserResponse(null, null, List.of(), false, null, "Bearer", 0);
    }
}
