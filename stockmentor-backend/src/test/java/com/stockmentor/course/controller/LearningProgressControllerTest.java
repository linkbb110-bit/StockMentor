package com.stockmentor.course.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.course.service.LearningProgressService;
import com.stockmentor.course.vo.CourseProgressResponse;
import com.stockmentor.course.vo.LessonCompletionResponse;
import com.stockmentor.course.vo.NextLessonResponse;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
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
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LearningProgressControllerTest {

    private static final long USER_ID = 42L;
    private static final AuthenticatedUser CURRENT_USER = new AuthenticatedUser(
            USER_ID,
            "learner@example.com",
            UserRole.USER
    );

    @Mock
    private LearningProgressService learningProgressService;

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
                .standaloneSetup(new LearningProgressController(learningProgressService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder()
                                .addModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
    void completionUsesOnlyTheAuthenticatedUserEvenWhenBodyContainsAnotherUserId()
            throws Exception {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 10, 11, 10, 30);
        when(learningProgressService.completeLesson(USER_ID, 101L))
                .thenReturn(new LessonCompletionResponse(101L, true, completedAt));

        mockMvc.perform(put("/api/v1/me/lessons/101/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": {
                            "lessonId": 101,
                            "completed": true,
                            "completedAt": "2026-08-10T11:10:30"
                          }
                        }
                        """, JsonCompareMode.STRICT));

        verify(learningProgressService).completeLesson(USER_ID, 101L);
    }

    @Test
    void progressReturnsTheApprovedPersistentStateContract() throws Exception {
        when(learningProgressService.getCourseProgress(USER_ID, 7L))
                .thenReturn(new CourseProgressResponse(
                        2L,
                        3L,
                        66,
                        List.of(101L, 103L),
                        new NextLessonResponse(
                                102L,
                                "第二课",
                                "第二课摘要",
                                8,
                                11L,
                                "第一章"
                        )
                ));

        mockMvc.perform(get("/api/v1/me/courses/7/progress"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": {
                            "completedLessons": 2,
                            "totalLessons": 3,
                            "progressPercent": 66,
                            "completedLessonIds": [101, 103],
                            "nextLesson": {
                              "id": 102,
                              "title": "第二课",
                              "summary": "第二课摘要",
                              "estimatedMinutes": 8,
                              "chapterId": 11,
                              "chapterTitle": "第一章"
                            }
                          }
                        }
                        """, JsonCompareMode.STRICT));

        verify(learningProgressService).getCourseProgress(USER_ID, 7L);
    }

    @Test
    void unpublishedLessonUsesUnifiedResourceNotFoundResponse() throws Exception {
        when(learningProgressService.completeLesson(USER_ID, 404L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(put("/api/v1/me/lessons/404/completion"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {
                          "code": "RESOURCE_NOT_FOUND",
                          "message": "资源不存在",
                          "data": null
                        }
                        """, JsonCompareMode.STRICT));
    }
}
