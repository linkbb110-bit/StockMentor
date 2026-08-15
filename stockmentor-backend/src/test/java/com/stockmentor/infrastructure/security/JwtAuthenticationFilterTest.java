package com.stockmentor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.user.domain.UserRole;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class JwtAuthenticationFilterTest {

    private static final long USER_ID = 42L;
    private static final String TOKEN = "header.payload.signature-sensitive-value";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SecurityUserService securityUserService;

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                securityUserService,
                objectMapper,
                new PublicCourseGetRequestMatcher()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthorizationContinuesWithAnEmptySecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider, securityUserService);
    }

    @Test
    void nonBearerAuthorizationReturnsPublicUtf8Json401WithoutContinuing() throws Exception {
        MockHttpServletRequest request = requestWithAuthorization("Basic credentials");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertPublicAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider, securityUserService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer    "})
    void blankBearerTokenReturnsPublicUtf8Json401WithoutContinuing(String authorization)
            throws Exception {
        MockHttpServletRequest request = requestWithAuthorization(authorization);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertPublicAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider, securityUserService);
    }

    @Test
    void validTokenUsesLatestDatabaseIdentityAsAuthenticationAndContinues() throws Exception {
        AuthenticatedUser latestIdentity =
                new AuthenticatedUser(USER_ID, "latest@example.com", UserRole.ADMIN);
        when(jwtTokenProvider.parseUserId(TOKEN)).thenReturn(USER_ID);
        when(securityUserService.load(USER_ID)).thenReturn(latestIdentity);
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(latestIdentity);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).parseUserId(TOKEN);
        verify(securityUserService).load(USER_ID);
    }

    @Test
    void validTokenReloadsDatabaseIdentityOnEveryRequest() throws Exception {
        when(jwtTokenProvider.parseUserId(TOKEN)).thenReturn(USER_ID);
        when(securityUserService.load(USER_ID))
                .thenReturn(new AuthenticatedUser(USER_ID, "first@example.com", UserRole.USER))
                .thenReturn(new AuthenticatedUser(USER_ID, "latest@example.com", UserRole.ADMIN));

        filter.doFilter(
                requestWithBearer(TOKEN),
                new MockHttpServletResponse(),
                filterChain
        );
        SecurityContextHolder.clearContext();
        filter.doFilter(
                requestWithBearer(TOKEN),
                new MockHttpServletResponse(),
                filterChain
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(jwtTokenProvider, org.mockito.Mockito.times(2)).parseUserId(TOKEN);
        verify(securityUserService, org.mockito.Mockito.times(2)).load(USER_ID);
    }

    @ParameterizedTest
    @EnumSource(
            value = ErrorCode.class,
            names = {"AUTH_USER_DISABLED", "AUTH_USER_NOT_FOUND"}
    )
    void unavailableTokenUserMapsInternalClassificationToPublicInvalidToken(
            ErrorCode internalError
    ) throws Exception {
        when(jwtTokenProvider.parseUserId(TOKEN)).thenReturn(USER_ID);
        when(securityUserService.load(USER_ID))
                .thenThrow(new BusinessException(internalError));
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertPublicAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformed-token", "altered.token.signature"})
    void malformedOrAlteredTokenReturnsPublicInvalidToken(String token) throws Exception {
        when(jwtTokenProvider.parseUserId(token))
                .thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
        MockHttpServletRequest request = requestWithBearer(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertPublicAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
        verifyNoInteractions(securityUserService);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void expiredTokenPreservesTheApprovedPublicExpiredClassification() throws Exception {
        when(jwtTokenProvider.parseUserId(TOKEN))
                .thenThrow(new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED));
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertPublicAuthenticationFailure(response, ErrorCode.AUTH_TOKEN_EXPIRED);
        verifyNoInteractions(securityUserService);
        verify(filterChain, never()).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/courses",
        "/api/v1/courses/7",
        "/api/v1/lessons/101",
        "/api/v1/lessons/101/quiz"
    })
    void publicCourseGetDoesNotParseAnInvalidOrExpiredBearerToken(String path)
            throws Exception {
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        request.setMethod("GET");
        request.setRequestURI(path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider, securityUserService);
    }

    @Test
    void authenticationFailureClearsAnyExistingSecurityContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "stale-principal",
                        null,
                        List.of()
                )
        );
        when(jwtTokenProvider.parseUserId(TOKEN))
                .thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertPublicAuthenticationFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    void unexpectedRuntimeFailureIsNotHiddenAsAnAuthenticationFailure() {
        when(jwtTokenProvider.parseUserId(TOKEN))
                .thenThrow(new IllegalStateException("database unavailable"));
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticationFailureDoesNotExposeOrLogTheFullToken(CapturedOutput output)
            throws Exception {
        when(jwtTokenProvider.parseUserId(TOKEN))
                .thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
        MockHttpServletRequest request = requestWithBearer(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getContentAsString()).doesNotContain(TOKEN);
        assertThat(output.getAll()).doesNotContain(TOKEN);
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        return requestWithAuthorization("Bearer " + token);
    }

    private MockHttpServletRequest requestWithAuthorization(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }

    private void assertPublicAuthenticationFailure(
            MockHttpServletResponse response,
            ErrorCode expectedError
    ) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getCharacterEncoding())
                .isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(MediaType.parseMediaType(response.getContentType())
                .isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("code").asText()).isEqualTo(expectedError.name());
        assertThat(body.path("message").asText()).isEqualTo(expectedError.message());
        assertThat(body.path("data").isNull()).isTrue();
    }
}
