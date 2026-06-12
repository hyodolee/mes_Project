package com.mcs.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}

