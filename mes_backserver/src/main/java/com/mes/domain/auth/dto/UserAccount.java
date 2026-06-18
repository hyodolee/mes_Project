package com.mes.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
    private String userId;
    private String password;
    private String displayName;
    private String role;
    private String useYn;
}
