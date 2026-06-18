package com.mes.interfaces.api.auth;

import com.mes.domain.auth.dto.AuthUserResponse;
import com.mes.domain.auth.dto.LoginRequest;
import com.mes.domain.auth.dto.UserAccount;
import com.mes.global.response.ApiResponse;
import com.mes.global.security.JwtTokenProvider;
import com.mes.infra.persistence.mybatis.mapper.auth.UserMapper;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserMapper userMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ApiResponse<AuthUserResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        return ApiResponse.ok(toResponse(authentication, true));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(Authentication authentication) {
        return ApiResponse.ok(toResponse(authentication, false));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        SecurityContextHolder.clearContext();
        return ApiResponse.ok();
    }

    private AuthUserResponse toResponse(Authentication authentication, boolean includeToken) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return AuthUserResponse.anonymous();
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String username = authentication.getName();
        UserAccount account = userMapper.findByUserId(username);
        String displayName = account != null ? account.getDisplayName() : username;
        String accessToken = includeToken ? jwtTokenProvider.generateToken(username, roles) : null;
        long expiresIn = includeToken ? jwtTokenProvider.expirationSeconds() : 0;
        return new AuthUserResponse(username, displayName, roles, true, accessToken, "Bearer", expiresIn);
    }
}
