package com.stockmentor.course.vo;

public record NextLessonResponse(
        long id,
        String title,
        String summary,
        int estimatedMinutes,
        long chapterId,
        String chapterTitle
) {
}
