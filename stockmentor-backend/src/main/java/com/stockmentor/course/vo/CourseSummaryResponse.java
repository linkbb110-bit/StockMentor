package com.stockmentor.course.vo;

public record CourseSummaryResponse(
        long id,
        String title,
        String summary,
        String coverUrl
) {
}
