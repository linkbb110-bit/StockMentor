package com.stockmentor.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmentor.infrastructure.security.AuthenticatedUser;
import com.stockmentor.infrastructure.security.JwtTokenProvider;
import com.stockmentor.infrastructure.security.SecurityUserService;
import com.stockmentor.course.mapper.ChapterMapper;
import com.stockmentor.course.mapper.CourseMapper;
import com.stockmentor.course.mapper.LearningProgressMapper;
import com.stockmentor.course.mapper.LessonMapper;
import com.stockmentor.quiz.mapper.QuestionMapper;
import com.stockmentor.quiz.mapper.QuestionOptionMapper;
import com.stockmentor.quiz.mapper.QuizAnswerMapper;
import com.stockmentor.quiz.mapper.QuizAnswerOptionMapper;
import com.stockmentor.quiz.mapper.QuizAttemptMapper;
import com.stockmentor.quiz.mapper.QuizMapper;
import com.stockmentor.quiz.mapper.QuizQuestionMapper;
import com.stockmentor.quiz.mapper.WrongQuestionMapper;
import com.stockmentor.user.domain.UserRole;
import com.stockmentor.user.dto.UpdateNicknameRequest;
import com.stockmentor.user.mapper.UserMapper;
import com.stockmentor.user.service.UserService;
import com.stockmentor.user.vo.CurrentUserResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc(
        print = MockMvcPrint.NONE,
        printOnlyOnFailure = false
)
@ActiveProfiles("test")
class CurrentUserControllerTest {

    private static final long USER_ID = 73L;
    private static final String EMAIL = "student@example.com";
    private static final String ORIGINAL_NICKNAME = "学习投资";
    private static final String UPDATED_NICKNAME = "长期学习者";
    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 7, 28, 20, 15, 30);
    private static final String ME_PATH = "/api/v1/users/me";
    private static final String NICKNAME_PATH =
            "/api/v1/users/me/nickname";
    private static final String AUTHENTICATION_FAILURE_JSON = """
            {
              "code": "AUTH_INVALID_TOKEN",
              "message": "登录状态已失效，请重新登录",
              "data": null
            }
            """;

    @MockitoBean
    private SecurityUserService securityUserService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CourseMapper courseMapper;

    @MockitoBean
    private ChapterMapper chapterMapper;

    @MockitoBean
    private LessonMapper lessonMapper;

    @MockitoBean
    private LearningProgressMapper learningProgressMapper;

    @MockitoBean
    private QuizMapper quizMapper;

    @MockitoBean
    private QuizQuestionMapper quizQuestionMapper;

    @MockitoBean
    private QuestionMapper questionMapper;

    @MockitoBean
    private QuestionOptionMapper questionOptionMapper;

    @MockitoBean
    private QuizAttemptMapper quizAttemptMapper;

    @MockitoBean
    private QuizAnswerMapper quizAnswerMapper;

    @MockitoBean
    private QuizAnswerOptionMapper quizAnswerOptionMapper;

    @MockitoBean
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void authenticatedGetReturnsTheExactSafeCurrentUserFromTheReloadedIdentity()
            throws Exception {
        stubReloadedIdentity();
        when(userService.getCurrentUser(USER_ID))
                .thenReturn(currentUser(ORIGINAL_NICKNAME));

        mockMvc.perform(get(ME_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(content().json(
                        currentUserJson(ORIGINAL_NICKNAME),
                        JsonCompareMode.STRICT
                ))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());

        verify(securityUserService).load(USER_ID);
        verify(userService).getCurrentUser(USER_ID);
    }

    @Test
    void anonymousGetReturnsTheExactUnifiedAuthenticationFailure()
            throws Exception {
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(content().json(
                        AUTHENTICATION_FAILURE_JSON,
                        JsonCompareMode.STRICT
                ));

        verifyNoInteractions(securityUserService, userService);
    }

    @Test
    void authenticatedPatchUpdatesTheNicknameForTheExactCurrentUser()
            throws Exception {
        stubReloadedIdentity();
        UpdateNicknameRequest request =
                new UpdateNicknameRequest("  " + UPDATED_NICKNAME + "  ");
        when(userService.updateNickname(USER_ID, request))
                .thenReturn(currentUser(UPDATED_NICKNAME));

        mockMvc.perform(patch(NICKNAME_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "  长期学习者  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(content().json(
                        currentUserJson(UPDATED_NICKNAME),
                        JsonCompareMode.STRICT
                ));

        verify(securityUserService).load(USER_ID);
        verify(userService).updateNickname(USER_ID, request);
    }

    @Test
    void updateNicknameRequestExposesOnlyTheApprovedNicknameComponent() {
        assertThat(Arrays.stream(
                        UpdateNicknameRequest.class.getRecordComponents()
                ).map(component -> component.getName()))
                .containsExactly("nickname");
        assertThat(Arrays.stream(
                        UpdateNicknameRequest.class.getRecordComponents()
                ).map(component -> component.getType().getName()))
                .containsExactly("java.lang.String");
    }

    @Test
    void extraProfileFieldsCannotChangeWhatTheNicknameServiceReceivesOrReturns()
            throws Exception {
        stubReloadedIdentity();
        UpdateNicknameRequest approvedRequest =
                new UpdateNicknameRequest(UPDATED_NICKNAME);
        when(userService.updateNickname(USER_ID, approvedRequest))
                .thenReturn(currentUser(UPDATED_NICKNAME));

        mockMvc.perform(patch(NICKNAME_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "长期学习者",
                                  "email": "attacker@example.com",
                                  "role": "ADMIN",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        currentUserJson(UPDATED_NICKNAME),
                        JsonCompareMode.STRICT
                ))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());

        verify(securityUserService).load(USER_ID);
        verify(userService).updateNickname(USER_ID, approvedRequest);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"nickname\":null}"
    })
    void missingOrNullNicknameReturnsValidationFailureWithoutCallingTheService(
            String requestJson
    ) throws Exception {
        stubReloadedIdentity();

        mockMvc.perform(patch(NICKNAME_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": "VALIDATION_FAILED",
                          "message": "请求参数不合法",
                          "data": null
                        }
                        """, JsonCompareMode.STRICT));

        verify(securityUserService).load(USER_ID);
        verifyNoInteractions(userService);
    }

    private void stubReloadedIdentity() {
        when(securityUserService.load(USER_ID))
                .thenReturn(new AuthenticatedUser(
                        USER_ID,
                        EMAIL,
                        UserRole.USER
                ));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.issue(USER_ID, Instant.now());
    }

    private CurrentUserResponse currentUser(String nickname) {
        return new CurrentUserResponse(
                USER_ID,
                EMAIL,
                nickname,
                UserRole.USER,
                CREATED_AT
        );
    }

    private String currentUserJson(String nickname) {
        return """
                {
                  "code": "SUCCESS",
                  "message": "操作成功",
                  "data": {
                    "id": 73,
                    "email": "student@example.com",
                    "nickname": "%s",
                    "role": "USER",
                    "createdAt": "2026-07-28T20:15:30"
                  }
                }
                """.formatted(nickname);
    }
}
