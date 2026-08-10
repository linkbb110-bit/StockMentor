package com.stockmentor.course.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.stockmentor.course.entity.ChapterEntity;
import com.stockmentor.course.entity.CourseEntity;
import com.stockmentor.course.entity.LessonEntity;
import com.stockmentor.course.mapper.ChapterMapper;
import com.stockmentor.course.mapper.CourseMapper;
import com.stockmentor.course.mapper.LessonMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyBatisCourseRepositoryTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private LessonMapper lessonMapper;

    private MyBatisCourseRepository repository;

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "course-test"),
                CourseEntity.class
        );
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "chapter-test"),
                ChapterEntity.class
        );
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "lesson-test"),
                LessonEntity.class
        );
    }

    @BeforeEach
    void setUp() {
        repository = new MyBatisCourseRepository(courseMapper, chapterMapper, lessonMapper);
    }

    @Test
    void courseListConstrainsPublishedAndUsesStableSortOrder() {
        when(courseMapper.selectList(any())).thenReturn(List.of(new CourseEntity()));

        assertThat(repository.findPublishedCourses()).hasSize(1);

        Wrapper<CourseEntity> wrapper = captureCourseListWrapper();
        assertThat(wrapper.getSqlSegment())
                .containsPattern("(?i)\\bpublished\\s*=")
                .containsPattern("(?i)ORDER BY\\s+sort_order\\s+ASC\\s*,\\s*id\\s+ASC");
        assertThat(parametersOf(wrapper)).containsValue(true);
    }

    @Test
    void courseDetailLookupRequiresBothRequestedIdAndPublishedFlag() {
        CourseEntity course = new CourseEntity();
        when(courseMapper.selectOne(any())).thenReturn(course);

        assertThat(repository.findPublishedCourseById(7L)).containsSame(course);

        Wrapper<CourseEntity> wrapper = captureCourseOneWrapper();
        assertThat(wrapper.getSqlSegment())
                .containsPattern("(?i)\\bid\\s*=")
                .containsPattern("(?i)\\bpublished\\s*=");
        assertThat(parametersOf(wrapper)).containsValues(7L, true);
    }

    @Test
    void chapterQueryUsesOneCourseConditionAndStableOrdering() {
        when(chapterMapper.selectList(any())).thenReturn(List.of(new ChapterEntity()));

        assertThat(repository.findChaptersByCourseId(7L)).hasSize(1);

        ArgumentCaptor<Wrapper<ChapterEntity>> captor = wrapperCaptor();
        verify(chapterMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .containsPattern("(?i)\\bcourse_id\\s*=")
                .containsPattern("(?i)ORDER BY\\s+sort_order\\s+ASC\\s*,\\s*id\\s+ASC");
        assertThat(parametersOf(captor.getValue())).containsValue(7L);
    }

    @Test
    void publishedLessonsForCourseUseOneBatchMapperCall() {
        LessonEntity lesson = new LessonEntity();
        when(lessonMapper.selectPublishedByCourseId(7L)).thenReturn(List.of(lesson));

        assertThat(repository.findPublishedLessonsByCourseId(7L)).containsExactly(lesson);

        verify(lessonMapper).selectPublishedByCourseId(7L);
    }

    @Test
    void lessonDetailDelegatesToThePublishedCourseAndLessonJoin() {
        PublishedLessonRow row = new PublishedLessonRow(
                101L,
                "股票是什么",
                "理解基本权利",
                "content",
                8,
                7L,
                "股票投资基础",
                11L,
                "基础概念"
        );
        when(lessonMapper.selectPublishedDetailsById(101L)).thenReturn(row);

        assertThat(repository.findPublishedLessonById(101L)).containsSame(row);

        verify(lessonMapper).selectPublishedDetailsById(101L);
    }

    private Wrapper<CourseEntity> captureCourseListWrapper() {
        ArgumentCaptor<Wrapper<CourseEntity>> captor = wrapperCaptor();
        verify(courseMapper).selectList(captor.capture());
        return captor.getValue();
    }

    private Wrapper<CourseEntity> captureCourseOneWrapper() {
        ArgumentCaptor<Wrapper<CourseEntity>> captor = wrapperCaptor();
        verify(courseMapper).selectOne(captor.capture());
        return captor.getValue();
    }

    private Map<String, Object> parametersOf(Wrapper<?> wrapper) {
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
