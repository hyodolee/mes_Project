package com.mes.global.security;

import com.mes.domain.auth.dto.UserAccount;
import com.mes.global.config.SeedUserProperties;
import com.mes.infra.persistence.mybatis.mapper.auth.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 기동 시 application.yml에 정의된 기본 계정을 MST_USER에 시드한다.
 * 이미 존재하는 계정은 건드리지 않으므로(멱등) 매 기동마다 안전하게 실행된다.
 */
@Component
public class UserAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserAccountSeeder.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SeedUserProperties seedUserProperties;

    public UserAccountSeeder(UserMapper userMapper, PasswordEncoder passwordEncoder, SeedUserProperties seedUserProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.seedUserProperties = seedUserProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedUserProperties.SeedUser seed : seedUserProperties.getSeedUsers()) {
            if (seed.getUsername() == null || seed.getUsername().isBlank()) {
                continue;
            }
            if (userMapper.countByUserId(seed.getUsername()) > 0) {
                continue;
            }
            userMapper.insertUser(new UserAccount(
                    seed.getUsername(),
                    passwordEncoder.encode(seed.getPassword()),
                    seed.getDisplayName() != null ? seed.getDisplayName() : seed.getUsername(),
                    seed.getRole() != null ? seed.getRole() : "USER",
                    "Y"
            ));
            log.info("기본 사용자 계정 생성: {} (role={})", seed.getUsername(), seed.getRole());
        }
    }
}
