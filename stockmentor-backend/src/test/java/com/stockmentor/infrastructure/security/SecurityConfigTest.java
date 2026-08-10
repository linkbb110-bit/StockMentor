package com.stockmentor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmentor.course.mapper.ChapterMapper;
import com.stockmentor.course.mapper.CourseMapper;
import com.stockmentor.course.mapper.LessonMapper;
import com.stockmentor.course.service.CourseQueryService;
import com.stockmentor.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
    "stockmentor.security.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String REJECTED_ORIGIN = "https://untrusted.example";
    private static final String AUTHENTICATION_FAILURE_JSON = """
            {
              "code": "AUTH_INVALID_TOKEN",
              "message": "登录状态已失效，请重新登录",
              "data": null
            }
            """;
    private static final String ACCESS_DENIED_JSON = """
            {
              "code": "FORBIDDEN",
              "message": "没有权限执行该操作",
              "data": null
            }
            """;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CourseMapper courseMapper;

    @MockitoBean
    private ChapterMapper chapterMapper;

    @MockitoBean
    private LessonMapper lessonMapper;

    @MockitoBean
    private CourseQueryService courseQueryService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void authenticationEntryPointWritesUnifiedUtf8Json401Directly() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("authentication required")
        );

        assertDirectJsonResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                AUTHENTICATION_FAILURE_JSON
        );
    }

    @Test
    void accessDeniedHandlerWritesUnifiedUtf8Json403Directly() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("access denied")
        );

        assertDirectJsonResponse(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                ACCESS_DENIED_JSON
        );
    }

    @Test
    void nullCorsWhitelistIsRejected() {
        assertThatThrownBy(() -> new CorsProperties(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CORS allowed origins must be an explicit non-empty whitelist");
    }

    @Test
    void emptyCorsWhitelistIsRejected() {
        assertThatThrownBy(() -> new CorsProperties(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CORS allowed origins must be an explicit non-empty whitelist");
    }

    @Test
    void corsWhitelistContainingNullIsRejected() {
        assertThatThrownBy(() -> new CorsProperties(Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CORS allowed origins must be an explicit non-empty whitelist");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "*", " * "})
    void blankOrWildcardCorsOriginIsRejected(String origin) {
        assertThatThrownBy(() -> new CorsProperties(List.of(origin)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CORS allowed origins must be an explicit non-empty whitelist");
    }

    @Test
    void corsWhitelistTrimsEveryExplicitOrigin() {
        CorsProperties properties = new CorsProperties(List.of(
                " http://localhost:5173 ",
                "https://app.example "
        ));

        assertThat(properties.allowedOrigins())
                .containsExactly("http://localhost:5173", "https://app.example");
    }

    @Test
    void commaSeparatedCorsPropertyBindsAsAnExplicitTrimmedWhitelist() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "stockmentor.security.cors.allowed-origins",
                " http://localhost:5173 , https://app.example "
        ));

        CorsProperties properties = new Binder(source)
                .bind(
                        "stockmentor.security.cors",
                        Bindable.of(CorsProperties.class)
                )
                .orElseThrow(() -> new AssertionError("CORS properties did not bind"));

        assertThat(properties.allowedOrigins())
                .containsExactly("http://localhost:5173", "https://app.example");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/register", "/api/v1/auth/login"})
    void anonymousAuthPostsAreNotBlockedBySecurityWhenControllerIsNotYetPresent(String path)
            throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(
                HttpServletResponse.SC_UNAUTHORIZED,
                HttpServletResponse.SC_FORBIDDEN
        );
    }

    @Test
    void healthEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk());
    }

    @Test
    void openApiEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedUserEndpointWithoutTokenReturnsExactUnifiedJson401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        AUTHENTICATION_FAILURE_JSON,
                        JsonCompareMode.STRICT
                ));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/courses",
        "/api/v1/courses/7",
        "/api/v1/lessons/101"
    })
    void publicCourseGetsAreAvailableWithoutAuthentication(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBearerDoesNotBlockAnonymousCourseReading() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void expiredBearerDoesNotBlockAnonymousLessonReading() throws Exception {
        String expiredToken = jwtTokenProvider.issue(
                42L,
                Instant.now().minusSeconds(7_201)
        );

        mockMvc.perform(get("/api/v1/lessons/101")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + expiredToken
                        ))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBearerStillFailsOnExistingPrivateEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(
                        AUTHENTICATION_FAILURE_JSON,
                        JsonCompareMode.STRICT
                ));
    }

    @Test
    void allowedPreflightUsesOnlyTheConfiguredMethodsAndHeadersWithoutCredentials()
            throws Exception {
        MvcResult result = mockMvc.perform(options("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type, Accept"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        ALLOWED_ORIGIN
                ))
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS
                ))
                .andReturn();

        assertThat(commaSeparatedHeader(
                result,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS
        )).containsExactlyInAnyOrder("GET", "POST", "PATCH", "OPTIONS");
        assertThat(commaSeparatedHeader(
                result,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
        )).containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept");
    }

    @Test
    void preflightFromOriginOutsideWhitelistIsRejectedWithoutAllowOriginHeader()
            throws Exception {
        mockMvc.perform(options("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, REJECTED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type, Accept"
                        ))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS
                ));
    }

    private void assertDirectJsonResponse(
            MockHttpServletResponse response,
            int expectedStatus,
            String expectedJson
    ) throws Exception {
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(MediaType.parseMediaType(response.getContentType())
                .isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(objectMapper.readTree(response.getContentAsByteArray()))
                .isEqualTo(objectMapper.readTree(expectedJson));
    }

    private Set<String> commaSeparatedHeader(MvcResult result, String name) {
        return Arrays.stream(result.getResponse().getHeader(name).split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
