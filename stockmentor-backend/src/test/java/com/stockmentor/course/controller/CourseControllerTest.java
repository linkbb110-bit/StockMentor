package com.stockmentor.course.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.course.service.CourseQueryService;
import com.stockmentor.course.vo.ChapterSummaryResponse;
import com.stockmentor.course.vo.CourseDetailResponse;
import com.stockmentor.course.vo.CourseSummaryResponse;
import com.stockmentor.course.vo.LessonSummaryResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseQueryService courseQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CourseController(courseQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsTheExactPublishedCourseSummaryContract() throws Exception {
        when(courseQueryService.listPublishedCourses()).thenReturn(List.of(
                new CourseSummaryResponse(7L, "股票投资基础", "从概念到复盘", null)
        ));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": [
                            {
                              "id": 7,
                              "title": "股票投资基础",
                              "summary": "从概念到复盘",
                              "coverUrl": null
                            }
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void detailReturnsOrderedChapterAndPublishedLessonSummaries() throws Exception {
        when(courseQueryService.getPublishedCourse(7L)).thenReturn(
                new CourseDetailResponse(
                        7L,
                        "股票投资基础",
                        "从概念到复盘",
                        null,
                        List.of(new ChapterSummaryResponse(
                                11L,
                                "基础概念",
                                "认识资产",
                                List.of(new LessonSummaryResponse(
                                        101L,
                                        "股票是什么",
                                        "理解基本权利",
                                        8
                                ))
                        ))
                )
        );

        mockMvc.perform(get("/api/v1/courses/7"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": {
                            "id": 7,
                            "title": "股票投资基础",
                            "summary": "从概念到复盘",
                            "coverUrl": null,
                            "chapters": [
                              {
                                "id": 11,
                                "title": "基础概念",
                                "summary": "认识资产",
                                "lessons": [
                                  {
                                    "id": 101,
                                    "title": "股票是什么",
                                    "summary": "理解基本权利",
                                    "estimatedMinutes": 8
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void unpublishedOrMissingCourseUsesTheUnifiedResourceNotFoundResponse() throws Exception {
        when(courseQueryService.getPublishedCourse(88L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/courses/88"))
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
