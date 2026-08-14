package com.stockmentor.quiz.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.dto.WrongQuestionAnswerRequest;
import com.stockmentor.quiz.service.WrongQuestionService;
import com.stockmentor.quiz.vo.QuizOptionResponse;
import com.stockmentor.quiz.vo.WrongQuestionResponse;
import com.stockmentor.quiz.vo.WrongQuestionReviewResponse;
import com.stockmentor.user.domain.UserRole;
import java.time.LocalDateTime;
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
class WrongQuestionControllerTest {

    private static final long USER_ID = 42L;
    private static final AuthenticatedUser CURRENT_USER = new AuthenticatedUser(
            USER_ID,
            "learner@example.com",
            UserRole.USER
    );

    @Mock
    private WrongQuestionService wrongQuestionService;

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
                .standaloneSetup(new WrongQuestionController(wrongQuestionService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder()
                                .addModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
    void listDefaultsToPendingAndDoesNotLeakAnswersOrExplanation() throws Exception {
        when(wrongQuestionService.list(USER_ID, null)).thenReturn(List.of(
                new WrongQuestionResponse(
                        31L,
                        101L,
                        "Stock basics",
                        QuestionType.SINGLE_CHOICE,
                        "What does a share represent?",
                        List.of(
                                new QuizOptionResponse(301L, "A", "Ownership"),
                                new QuizOptionResponse(302L, "B", "A guaranteed return")
                        ),
                        WrongQuestionStatus.PENDING,
                        2,
                        LocalDateTime.of(2026, 8, 14, 10, 30),
                        null
                )
        ));

        mockMvc.perform(get("/api/v1/me/wrong-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionId").value(31))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].correctOptionIds").doesNotExist())
                .andExpect(jsonPath("$.data[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.data[0].options[*].correct").doesNotExist())
                .andExpect(jsonPath("$.data[0].options[*].isCorrect").doesNotExist());

        verify(wrongQuestionService).list(USER_ID, null);
    }

    @Test
    void explicitStatusIsPassedWithAuthenticatedUserId() throws Exception {
        when(wrongQuestionService.list(USER_ID, "MASTERED")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/me/wrong-questions")
                        .queryParam("status", "MASTERED"))
                .andExpect(status().isOk());

        verify(wrongQuestionService).list(USER_ID, "MASTERED");
    }

    @Test
    void reviewUsesOnlyAuthenticatedUserAndReturnsLatestState() throws Exception {
        WrongQuestionAnswerRequest request =
                new WrongQuestionAnswerRequest(List.of(301L));
        when(wrongQuestionService.review(USER_ID, 31L, request))
                .thenReturn(new WrongQuestionReviewResponse(
                        31L,
                        true,
                        WrongQuestionStatus.MASTERED,
                        2,
                        List.of(301L),
                        "Ownership represents a share in a company."
                ));

        mockMvc.perform(post("/api/v1/me/wrong-questions/31/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999,
                                  "selectedOptionIds": [301]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.questionId").value(31))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.status").value("MASTERED"))
                .andExpect(jsonPath("$.data.errorCount").value(2))
                .andExpect(jsonPath("$.data.correctOptionIds[0]").value(301))
                .andExpect(jsonPath("$.data.explanation")
                        .value("Ownership represents a share in a company."));

        verify(wrongQuestionService).review(USER_ID, 31L, request);
    }

    @Test
    void invalidStatusUsesUnified400() throws Exception {
        when(wrongQuestionService.list(USER_ID, "INVALID"))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

        mockMvc.perform(get("/api/v1/me/wrong-questions")
                        .queryParam("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersWrongQuestionIsIndistinguishableFromMissing() throws Exception {
        WrongQuestionAnswerRequest request =
                new WrongQuestionAnswerRequest(List.of(301L));
        when(wrongQuestionService.review(USER_ID, 31L, request))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(post("/api/v1/me/wrong-questions/31/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionIds\":[301]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedReviewIsRejectedBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/me/wrong-questions/31/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionIds\":null}"))
                .andExpect(status().isBadRequest());
    }
}
