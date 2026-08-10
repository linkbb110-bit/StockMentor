package com.stockmentor.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.course.entity.ChapterEntity;
import com.stockmentor.course.entity.CourseEntity;
import com.stockmentor.course.entity.LessonEntity;
import com.stockmentor.course.repository.CourseRepository;
import com.stockmentor.course.repository.PublishedLessonRow;
import com.stockmentor.course.vo.ChapterSummaryResponse;
import com.stockmentor.course.vo.LessonSummaryResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseQueryServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseQueryService service;

    @BeforeEach
    void setUp() {
        service = new CourseQueryService(courseRepository);
    }

    @Test
    void listMapsThePublishedCoursesReturnedByTheRepository() {
        when(courseRepository.findPublishedCourses()).thenReturn(List.of(
                course(7L, "股票投资基础", "从概念到复盘", null, 1),
                course(9L, "第二门课程", "用于稳定排序验证", "cover.png", 2)
        ));

        var result = service.listPublishedCourses();

        assertThat(result).extracting("id", "title", "summary", "coverUrl")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                7L,
                                "股票投资基础",
                                "从概念到复盘",
                                null
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                9L,
                                "第二门课程",
                                "用于稳定排序验证",
                                "cover.png"
                        )
                );
        verify(courseRepository).findPublishedCourses();
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void courseDetailUsesOneCourseOneChapterBatchAndOneLessonBatchAndKeepsEmptyChapters() {
        CourseEntity course = course(7L, "股票投资基础", "从概念到复盘", null, 1);
        ChapterEntity first = chapter(11L, 7L, "基础概念", "认识资产", 1);
        ChapterEntity second = chapter(12L, 7L, "收益与风险", "理解边界", 2);
        LessonEntity lesson = lesson(101L, 11L, "股票是什么", "理解基本权利", 1);
        when(courseRepository.findPublishedCourseById(7L)).thenReturn(Optional.of(course));
        when(courseRepository.findChaptersByCourseId(7L)).thenReturn(List.of(first, second));
        when(courseRepository.findPublishedLessonsByCourseId(7L)).thenReturn(List.of(lesson));

        var result = service.getPublishedCourse(7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.chapters())
                .extracting(ChapterSummaryResponse::id)
                .containsExactly(11L, 12L);
        assertThat(result.chapters().get(0).lessons())
                .extracting(LessonSummaryResponse::id)
                .containsExactly(101L);
        assertThat(result.chapters().get(1).lessons()).isEmpty();
        verify(courseRepository).findPublishedCourseById(7L);
        verify(courseRepository).findChaptersByCourseId(7L);
        verify(courseRepository).findPublishedLessonsByCourseId(7L);
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void unpublishedOrMissingCourseStopsBeforeChapterAndLessonQueries() {
        when(courseRepository.findPublishedCourseById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublishedCourse(88L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(courseRepository).findPublishedCourseById(88L);
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void lessonDetailMapsOnlyTheApprovedPublishedProjection() {
        PublishedLessonRow row = new PublishedLessonRow(
                101L,
                "股票是什么",
                "理解基本权利",
                "## 概念\n股票代表所有权的一部分。",
                8,
                7L,
                "股票投资基础",
                11L,
                "基础概念"
        );
        when(courseRepository.findPublishedLessonById(101L)).thenReturn(Optional.of(row));

        var result = service.getPublishedLesson(101L);

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.contentMd()).contains("## 概念");
        assertThat(result.courseId()).isEqualTo(7L);
        assertThat(result.courseTitle()).isEqualTo("股票投资基础");
        assertThat(result.chapterId()).isEqualTo(11L);
        assertThat(result.chapterTitle()).isEqualTo("基础概念");
        verify(courseRepository).findPublishedLessonById(101L);
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void unpublishedOrMissingLessonReturnsResourceNotFound() {
        when(courseRepository.findPublishedLessonById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublishedLesson(404L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private CourseEntity course(
            long id,
            String title,
            String summary,
            String coverUrl,
            int sortOrder
    ) {
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setCoverUrl(coverUrl);
        entity.setPublished(true);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private ChapterEntity chapter(
            long id,
            long courseId,
            String title,
            String summary,
            int sortOrder
    ) {
        ChapterEntity entity = new ChapterEntity();
        entity.setId(id);
        entity.setCourseId(courseId);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private LessonEntity lesson(
            long id,
            long chapterId,
            String title,
            String summary,
            int sortOrder
    ) {
        LessonEntity entity = new LessonEntity();
        entity.setId(id);
        entity.setChapterId(chapterId);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setEstimatedMinutes(8);
        entity.setSortOrder(sortOrder);
        entity.setPublished(true);
        return entity;
    }
}
