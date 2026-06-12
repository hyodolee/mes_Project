package com.mes.interfaces.api.auth;

import com.mes.domain.auth.dto.AuthUserResponse;
import com.mes.domain.auth.dto.LoginRequest;
import com.mes.global.response.ApiResponse;
import com.mes.global.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final String displayName;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.security.display-name}") String displayName
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.displayName = displayName;
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
        String accessToken = includeToken ? jwtTokenProvider.generateToken(authentication.getName(), roles) : null;
        long expiresIn = includeToken ? jwtTokenProvider.expirationSeconds() : 0;
        return new AuthUserResponse(authentication.getName(), displayName, roles, true, accessToken, "Bearer", expiresIn);
    }
}
