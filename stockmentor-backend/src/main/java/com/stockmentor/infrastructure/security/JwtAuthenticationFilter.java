package com.stockmentor.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityUserService securityUserService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SecurityUserService securityUserService,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityUserService = securityUserService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        if (!authorization.startsWith(BEARER_PREFIX)) {
            writeAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            writeAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
            return;
        }

        AuthenticatedUser authenticatedUser;
        try {
            long userId = jwtTokenProvider.parseUserId(token);
            authenticatedUser = securityUserService.load(userId);
        } catch (BusinessException exception) {
            ErrorCode publicError = publicAuthenticationError(exception.getErrorCode());
            if (publicError == null) {
                throw exception;
            }
            SecurityContextHolder.clearContext();
            writeAuthenticationFailure(response, publicError);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        authenticatedUser,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + authenticatedUser.role().name()
                        ))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private ErrorCode publicAuthenticationError(ErrorCode internalError) {
        return switch (internalError) {
            case AUTH_TOKEN_EXPIRED -> ErrorCode.AUTH_TOKEN_EXPIRED;
            case AUTH_INVALID_TOKEN, AUTH_USER_DISABLED, AUTH_USER_NOT_FOUND ->
                    ErrorCode.AUTH_INVALID_TOKEN;
            default -> null;
        };
    }

    private void writeAuthenticationFailure(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(errorCode.name(), errorCode.message())
        );
    }
}
