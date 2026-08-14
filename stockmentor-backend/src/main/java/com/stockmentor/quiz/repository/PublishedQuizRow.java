package com.stockmentor.quiz.repository;

public record PublishedQuizRow(
        long quizId,
        long lessonId,
        String lessonTitle,
        long courseId,
        String courseTitle,
        long chapterId,
        String chapterTitle,
        String title,
        String summary
) {
}
