package com.mes.global.security;

import com.mes.domain.auth.dto.UserAccount;
import com.mes.infra.persistence.mybatis.mapper.auth.UserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public DbUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userMapper.findByUserId(username);
        if (account == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }
        return User.withUsername(account.getUserId())
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }
}
