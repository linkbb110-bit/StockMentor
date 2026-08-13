package com.stockmentor.course.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.course.service.CourseQueryService;
import com.stockmentor.course.vo.LessonDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

    @Mock
    private CourseQueryService courseQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LessonController(courseQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void detailReturnsMarkdownAndOnlyApprovedCourseChapterContext() throws Exception {
        when(courseQueryService.getPublishedLesson(101L)).thenReturn(
                new LessonDetailResponse(
                        101L,
                        "股票是什么",
                        "理解基本权利",
                        "## 概念\n股票代表所有权的一部分。",
                        8,
                        7L,
                        "股票投资基础",
                        11L,
                        "基础概念"
                )
        );

        mockMvc.perform(get("/api/v1/lessons/101"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": {
                            "id": 101,
                            "title": "股票是什么",
                            "summary": "理解基本权利",
                            "contentMd": "## 概念\n股票代表所有权的一部分。",
                            "estimatedMinutes": 8,
                            "courseId": 7,
                            "courseTitle": "股票投资基础",
                            "chapterId": 11,
                            "chapterTitle": "基础概念"
                          }
                        }
                        """, JsonCompareMode.STRICT))
                .andExpect(jsonPath("$.data.published").doesNotExist())
                .andExpect(jsonPath("$.data.progress").doesNotExist());
    }

    @Test
    void unpublishedLessonOrLessonInsideUnpublishedCourseReturns404() throws Exception {
        when(courseQueryService.getPublishedLesson(404L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/lessons/404"))
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
