package com.stockmentor.course.vo;

import java.util.List;

public record CourseDetailResponse(
        long id,
        String title,
        String summary,
        String coverUrl,
        List<ChapterSummaryResponse> chapters
) {
}
