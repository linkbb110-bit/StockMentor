package com.stockmentor.course.vo;

import java.util.List;

public record ChapterSummaryResponse(
        long id,
        String title,
        String summary,
        List<LessonSummaryResponse> lessons
) {
}
