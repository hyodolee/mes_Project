package com.mes.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * application.yml의 app.security.seed-users 목록을 바인딩한다.
 * 애플리케이션 최초 기동 시 MST_USER에 없는 계정을 생성하는 데 사용된다.
 */
@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SeedUserProperties {

    private List<SeedUser> seedUsers = new ArrayList<>();

    @Getter
    @Setter
    public static class SeedUser {
        private String username;
        private String password;
        private String displayName;
        private String role;
    }
}
