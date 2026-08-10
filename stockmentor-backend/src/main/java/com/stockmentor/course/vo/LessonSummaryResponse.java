package com.stockmentor.course.vo;

public record LessonSummaryResponse(
        long id,
        String title,
        String summary,
        int estimatedMinutes
) {
}
