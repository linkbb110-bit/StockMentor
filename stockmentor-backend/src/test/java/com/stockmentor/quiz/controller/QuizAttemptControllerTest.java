package com.stockmentor.quiz.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
import com.stockmentor.quiz.dto.QuizAnswerRequest;
import com.stockmentor.quiz.dto.QuizAttemptRequest;
import com.stockmentor.quiz.service.QuizAttemptService;
import com.stockmentor.quiz.vo.QuizAttemptResponse;
import com.stockmentor.quiz.vo.QuizQuestionResultResponse;
import com.stockmentor.user.domain.UserRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class QuizAttemptControllerTest {

    private static final long USER_ID = 42L;
    private static final AuthenticatedUser CURRENT_USER = new AuthenticatedUser(
            USER_ID,
            "learner@example.com",
            UserRole.USER
    );

    @Mock
    private QuizAttemptService quizAttemptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        CURRENT_USER,
                        null,
                        List.of()
                )
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuizAttemptController(quizAttemptService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder()
                                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                .build()
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submissionReturns201AndUsesOnlyAuthenticatedUserId() throws Exception {
        QuizAttemptRequest request = new QuizAttemptRequest(List.of(
                new QuizAnswerRequest(31L, List.of(302L)),
                new QuizAnswerRequest(32L, List.of(311L))
        ));
        when(quizAttemptService.submit(USER_ID, 7L, request))
                .thenReturn(new QuizAttemptResponse(
                        123L,
                        2,
                        1,
                        50,
                        List.of(
                                new QuizQuestionResultResponse(
                                        31L,
                                        false,
                                        List.of(302L),
                                        List.of(301L),
                                        "Ownership represents a share in a company."
                                ),
                                new QuizQuestionResultResponse(
                                        32L,
                                        true,
                                        List.of(311L),
                                        List.of(311L),
                                        "The statement is true."
                                )
                        )
                ));

        mockMvc.perform(post("/api/v1/me/quizzes/7/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999,
                                  "answers": [
                                    {"questionId": 31, "selectedOptionIds": [302]},
                                    {"questionId": 32, "selectedOptionIds": [311]}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.attemptId").value(123))
                .andExpect(jsonPath("$.data.totalQuestions").value(2))
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.scorePercent").value(50))
                .andExpect(jsonPath("$.data.results[0].questionId").value(31))
                .andExpect(jsonPath("$.data.results[0].correct").value(false))
                .andExpect(jsonPath("$.data.results[0].selectedOptionIds[0]")
                        .value(302))
                .andExpect(jsonPath("$.data.results[0].correctOptionIds[0]")
                        .value(301))
                .andExpect(jsonPath("$.data.results[1].questionId").value(32))
                .andExpect(jsonPath("$.data.results[1].correct").value(true));

        verify(quizAttemptService).submit(USER_ID, 7L, request);
    }

    @Test
    void malformedSubmissionIsRejectedBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/me/quizzes/7/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
