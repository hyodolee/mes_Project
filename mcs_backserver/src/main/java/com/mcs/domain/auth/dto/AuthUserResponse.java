package com.mcs.domain.auth.dto;

import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
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

