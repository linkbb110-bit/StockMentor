package com.stockmentor.quiz.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.repository.PublicQuizOptionRow;
import com.stockmentor.quiz.repository.PublicQuizQuestionRow;
import com.stockmentor.quiz.repository.PublishedQuizRow;
import com.stockmentor.quiz.repository.QuizRepository;
import com.stockmentor.quiz.vo.PublicQuizResponse;
import com.stockmentor.quiz.vo.QuizOptionResponse;
import com.stockmentor.quiz.vo.QuizQuestionResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuizQueryService {

    private final QuizRepository quizRepository;

    public QuizQueryService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public PublicQuizResponse getPublishedQuiz(long lessonId) {
        PublishedQuizRow quiz = quizRepository.findPublishedByLessonId(lessonId)
                .orElseThrow(this::resourceNotFound);
        List<PublicQuizQuestionRow> questions =
                quizRepository.findPublishedQuestions(quiz.quizId());
        if (questions.isEmpty()) {
            throw resourceNotFound();
        }

        List<Long> questionIds = questions.stream()
                .map(PublicQuizQuestionRow::questionId)
                .toList();
        List<PublicQuizOptionRow> options = quizRepository.findPublishedOptions(questionIds);
        Map<Long, List<QuizOptionResponse>> optionsByQuestion = new LinkedHashMap<>();
        for (PublicQuizOptionRow option : options) {
            optionsByQuestion
                    .computeIfAbsent(option.questionId(), ignored -> new ArrayList<>())
                    .add(new QuizOptionResponse(
                            option.optionId(),
                            option.optionKey(),
                            option.content()
                    ));
        }

        List<QuizQuestionResponse> questionResponses = questions.stream()
                .map(question -> new QuizQuestionResponse(
                        question.questionId(),
                        question.type(),
                        question.stem(),
                        List.copyOf(optionsByQuestion.getOrDefault(
                                question.questionId(),
                                List.of()
                        ))
                ))
                .toList();

        return new PublicQuizResponse(
                quiz.quizId(),
                quiz.lessonId(),
                quiz.lessonTitle(),
                quiz.courseId(),
                quiz.courseTitle(),
                quiz.chapterId(),
                quiz.chapterTitle(),
                quiz.title(),
                quiz.summary(),
                questionResponses
        );
    }

    private BusinessException resourceNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
