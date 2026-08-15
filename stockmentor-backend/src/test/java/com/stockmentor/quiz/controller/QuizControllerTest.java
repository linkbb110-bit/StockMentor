package com.stockmentor.quiz.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.service.QuizQueryService;
import com.stockmentor.quiz.vo.PublicQuizResponse;
import com.stockmentor.quiz.vo.QuizOptionResponse;
import com.stockmentor.quiz.vo.QuizQuestionResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock
    private QuizQueryService quizQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuizController(quizQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publicResponseSerializesQuestionsWithoutAnyAnswerOrExplanationField() throws Exception {
        when(quizQueryService.getPublishedQuiz(101L)).thenReturn(new PublicQuizResponse(
                7L,
                101L,
                "股票是什么",
                1L,
                "股票投资基础",
                11L,
                "基础概念",
                "课后测验",
                "检验核心概念",
                List.of(new QuizQuestionResponse(
                        31L,
                        QuestionType.SINGLE_CHOICE,
                        "股票通常代表什么？",
                        List.of(
                                new QuizOptionResponse(301L, "A", "公司所有权的一部分"),
                                new QuizOptionResponse(302L, "B", "固定收益承诺")
                        )
                ))
        ));

        mockMvc.perform(get("/api/v1/lessons/101/quiz"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": {
                            "id": 7,
                            "lessonId": 101,
                            "lessonTitle": "股票是什么",
                            "courseId": 1,
                            "courseTitle": "股票投资基础",
                            "chapterId": 11,
                            "chapterTitle": "基础概念",
                            "title": "课后测验",
                            "summary": "检验核心概念",
                            "questions": [{
                              "questionId": 31,
                              "type": "SINGLE_CHOICE",
                              "stem": "股票通常代表什么？",
                              "options": [
                                {"optionId": 301, "optionKey": "A", "content": "公司所有权的一部分"},
                                {"optionId": 302, "optionKey": "B", "content": "固定收益承诺"}
                              ]
                            }]
                          }
                        }
                        """, JsonCompareMode.STRICT))
                .andExpect(jsonPath("$.data.questions[*].explanation").doesNotExist())
                .andExpect(jsonPath("$.data.questions[*].correctOptionIds").doesNotExist())
                .andExpect(jsonPath("$.data.questions[*].options[*].isCorrect").doesNotExist());
    }

    @Test
    void nonexistentOrUnpublishedQuizReturnsTheSame404() throws Exception {
        when(quizQueryService.getPublishedQuiz(404L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/lessons/404/quiz"))
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
