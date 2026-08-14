package com.stockmentor.system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmentor.course.mapper.ChapterMapper;
import com.stockmentor.course.mapper.CourseMapper;
import com.stockmentor.course.mapper.LearningProgressMapper;
import com.stockmentor.course.mapper.LessonMapper;
import com.stockmentor.quiz.mapper.QuestionOptionMapper;
import com.stockmentor.quiz.mapper.QuizAnswerMapper;
import com.stockmentor.quiz.mapper.QuizAnswerOptionMapper;
import com.stockmentor.quiz.mapper.QuizAttemptMapper;
import com.stockmentor.quiz.mapper.QuizMapper;
import com.stockmentor.quiz.mapper.QuizQuestionMapper;
import com.stockmentor.quiz.mapper.WrongQuestionMapper;
import com.stockmentor.user.mapper.UserMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

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

    @Test
    void healthEndpointReturnsThePublicServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "code": "SUCCESS",
                          "message": "操作成功",
                          "data": {
                            "status": "UP",
                            "service": "stockmentor-backend"
                          }
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void protectedEndpointRejectsAnonymousRequestsWithUnifiedJson401() throws Exception {
        mockMvc.perform(get("/api/v1/system/not-public"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "code": "AUTH_INVALID_TOKEN",
                          "message": "登录状态已失效，请重新登录",
                          "data": null
                        }
                        """, JsonCompareMode.STRICT));
    }
}
