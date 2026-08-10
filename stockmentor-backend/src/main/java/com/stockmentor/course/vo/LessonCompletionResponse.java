package com.stockmentor.course.vo;

import java.time.LocalDateTime;

public record LessonCompletionResponse(
        long lessonId,
        boolean completed,
        LocalDateTime completedAt
) {
}
