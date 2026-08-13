package com.stockmentor;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockmentor.course.mapper.ChapterMapper;
import com.stockmentor.course.mapper.CourseMapper;
import com.stockmentor.course.mapper.LearningProgressMapper;
import com.stockmentor.course.mapper.LessonMapper;
import com.stockmentor.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StringUtils;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
class StockMentorApplicationTests {

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

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void credentialFreeBaselineDoesNotAutoConfigureAUserDetailsService() {
        assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).isEmpty();
    }

    @Test
    void applicationStartupDoesNotLogAGeneratedSecurityPassword(CapturedOutput output) {
        assertThat(StringUtils.countOccurrencesOf(
                output.getAll(),
                "Using generated security password"
        )).isZero();
    }
}
