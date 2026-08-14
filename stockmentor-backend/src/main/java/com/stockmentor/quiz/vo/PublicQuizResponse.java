package com.stockmentor.quiz.vo;

import java.util.List;

public record PublicQuizResponse(
        long id,
        long lessonId,
        String lessonTitle,
        long courseId,
        String courseTitle,
        long chapterId,
        String chapterTitle,
        String title,
        String summary,
        List<QuizQuestionResponse> questions
) {
}
