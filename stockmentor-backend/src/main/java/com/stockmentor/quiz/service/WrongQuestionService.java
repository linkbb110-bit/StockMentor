package com.stockmentor.quiz.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.dto.WrongQuestionAnswerRequest;
import com.stockmentor.quiz.entity.WrongQuestionEntity;
import com.stockmentor.quiz.repository.PublicQuizOptionRow;
import com.stockmentor.quiz.repository.QuizRepository;
import com.stockmentor.quiz.repository.QuizScoringOptionRow;
import com.stockmentor.quiz.repository.QuizScoringQuestionRow;
import com.stockmentor.quiz.repository.WrongQuestionRepository;
import com.stockmentor.quiz.repository.WrongQuestionRow;
import com.stockmentor.quiz.scoring.ObjectiveQuestionScorer;
import com.stockmentor.quiz.scoring.ObjectiveScore;
import com.stockmentor.quiz.scoring.ScoringOption;
import com.stockmentor.quiz.vo.QuizOptionResponse;
import com.stockmentor.quiz.vo.WrongQuestionResponse;
import com.stockmentor.quiz.vo.WrongQuestionReviewResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WrongQuestionService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final QuizRepository quizRepository;
    private final ObjectiveQuestionScorer scorer;
    private final Clock clock;

    @Autowired
    public WrongQuestionService(
            WrongQuestionRepository wrongQuestionRepository,
            QuizRepository quizRepository,
            ObjectiveQuestionScorer scorer
    ) {
        this(
                wrongQuestionRepository,
                quizRepository,
                scorer,
                Clock.systemDefaultZone()
        );
    }

    WrongQuestionService(
            WrongQuestionRepository wrongQuestionRepository,
            QuizRepository quizRepository,
            ObjectiveQuestionScorer scorer,
            Clock clock
    ) {
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.quizRepository = quizRepository;
        this.scorer = scorer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WrongQuestionResponse> list(long userId, String requestedStatus) {
        WrongQuestionStatus status = parseStatus(requestedStatus);
        List<WrongQuestionRow> rows =
                wrongQuestionRepository.findVisibleByStatus(userId, status);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = rows.stream()
                .map(WrongQuestionRow::questionId)
                .toList();
        Map<Long, List<QuizOptionResponse>> optionsByQuestion = new LinkedHashMap<>();
        for (PublicQuizOptionRow option : quizRepository.findPublishedOptions(questionIds)) {
            optionsByQuestion.computeIfAbsent(
                    option.questionId(),
                    ignored -> new ArrayList<>()
            ).add(new QuizOptionResponse(
                    option.optionId(),
                    option.optionKey(),
                    option.content()
            ));
        }
        return rows.stream()
                .map(row -> new WrongQuestionResponse(
                        row.questionId(),
                        row.lessonId(),
                        row.lessonTitle(),
                        row.type(),
                        row.stem(),
                        List.copyOf(optionsByQuestion.getOrDefault(
                                row.questionId(),
                                List.of()
                        )),
                        row.status(),
                        row.errorCount(),
                        row.lastWrongAt(),
                        row.masteredAt()
                ))
                .toList();
    }

    @Transactional
    public WrongQuestionReviewResponse review(
            long userId,
            long questionId,
            WrongQuestionAnswerRequest request
    ) {
        wrongQuestionRepository.findVisibleByQuestion(userId, questionId)
                .orElseThrow(this::resourceNotFound);
        QuizScoringQuestionRow question = quizRepository
                .findScoringQuestion(questionId)
                .orElseThrow(this::resourceNotFound);
        List<QuizScoringOptionRow> optionRows =
                quizRepository.findScoringOptions(List.of(questionId));
        if (request == null || request.selectedOptionIds() == null) {
            throw validationFailed();
        }
        ObjectiveScore score = scorer.score(
                question.type(),
                request.selectedOptionIds(),
                optionRows.stream()
                        .map(option -> new ScoringOption(
                                option.optionId(),
                                option.correct(),
                                option.sortOrder()
                        ))
                        .toList()
        );
        LocalDateTime reviewedAt = LocalDateTime.now(clock);
        if (score.correct()) {
            wrongQuestionRepository.recordCorrect(userId, questionId, reviewedAt);
        } else {
            wrongQuestionRepository.recordWrong(userId, questionId, reviewedAt);
        }
        WrongQuestionEntity state = wrongQuestionRepository
                .findState(userId, questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        return new WrongQuestionReviewResponse(
                questionId,
                score.correct(),
                state.getStatus(),
                state.getErrorCount(),
                score.correctOptionIds(),
                question.explanation()
        );
    }

    private WrongQuestionStatus parseStatus(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return WrongQuestionStatus.PENDING;
        }
        try {
            return WrongQuestionStatus.valueOf(requestedStatus);
        } catch (IllegalArgumentException exception) {
            throw validationFailed();
        }
    }

    private BusinessException validationFailed() {
        return new BusinessException(ErrorCode.VALIDATION_FAILED);
    }

    private BusinessException resourceNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
