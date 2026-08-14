package com.stockmentor.quiz.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.dto.QuizAnswerRequest;
import com.stockmentor.quiz.dto.QuizAttemptRequest;
import com.stockmentor.quiz.entity.QuizAnswerEntity;
import com.stockmentor.quiz.entity.QuizAnswerOptionEntity;
import com.stockmentor.quiz.entity.QuizAttemptEntity;
import com.stockmentor.quiz.repository.QuizAttemptRepository;
import com.stockmentor.quiz.repository.QuizRepository;
import com.stockmentor.quiz.repository.QuizScoringOptionRow;
import com.stockmentor.quiz.repository.QuizScoringQuestionRow;
import com.stockmentor.quiz.repository.WrongQuestionRepository;
import com.stockmentor.quiz.scoring.ObjectiveQuestionScorer;
import com.stockmentor.quiz.scoring.ObjectiveScore;
import com.stockmentor.quiz.scoring.ScoringOption;
import com.stockmentor.quiz.vo.QuizAttemptResponse;
import com.stockmentor.quiz.vo.QuizQuestionResultResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final ObjectiveQuestionScorer scorer;
    private final Clock clock;

    @Autowired
    public QuizAttemptService(
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            WrongQuestionRepository wrongQuestionRepository,
            ObjectiveQuestionScorer scorer
    ) {
        this(
                quizRepository,
                attemptRepository,
                wrongQuestionRepository,
                scorer,
                Clock.systemDefaultZone()
        );
    }

    QuizAttemptService(
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            WrongQuestionRepository wrongQuestionRepository,
            ObjectiveQuestionScorer scorer,
            Clock clock
    ) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.scorer = scorer;
        this.clock = clock;
    }

    @Transactional
    public QuizAttemptResponse submit(
            long userId,
            long quizId,
            QuizAttemptRequest request
    ) {
        quizRepository.findPublishedByQuizId(quizId)
                .orElseThrow(this::resourceNotFound);
        List<QuizScoringQuestionRow> questions =
                quizRepository.findScoringQuestions(quizId);
        if (questions.isEmpty()) {
            throw resourceNotFound();
        }
        List<Long> questionIds = questions.stream()
                .map(QuizScoringQuestionRow::questionId)
                .toList();
        List<QuizScoringOptionRow> options =
                quizRepository.findScoringOptions(questionIds);
        List<PreparedResult> prepared = prepareResults(request, questions, options);

        int correctCount = (int) prepared.stream()
                .filter(result -> result.score().correct())
                .count();
        int totalQuestions = prepared.size();
        int scorePercent = correctCount == totalQuestions
                ? 100
                : correctCount * 100 / totalQuestions;
        LocalDateTime submittedAt = LocalDateTime.now(clock);
        QuizAttemptEntity attempt = attempt(
                userId,
                quizId,
                totalQuestions,
                correctCount,
                scorePercent,
                submittedAt
        );
        long attemptId = attemptRepository.createAttempt(attempt);

        List<QuizQuestionResultResponse> responses = new ArrayList<>();
        for (PreparedResult result : prepared) {
            long answerId = attemptRepository.createAnswer(answer(
                    attemptId,
                    result.question().questionId(),
                    result.score().correct()
            ));
            attemptRepository.createAnswerOptions(result.score().selectedOptionIds().stream()
                    .map(optionId -> selectedOption(answerId, optionId))
                    .toList());
            if (result.score().correct()) {
                wrongQuestionRepository.recordCorrect(
                        userId,
                        result.question().questionId(),
                        submittedAt
                );
            } else {
                wrongQuestionRepository.recordWrong(
                        userId,
                        result.question().questionId(),
                        submittedAt
                );
            }
            responses.add(new QuizQuestionResultResponse(
                    result.question().questionId(),
                    result.score().correct(),
                    result.score().selectedOptionIds(),
                    result.score().correctOptionIds(),
                    result.question().explanation()
            ));
        }

        return new QuizAttemptResponse(
                attemptId,
                totalQuestions,
                correctCount,
                scorePercent,
                List.copyOf(responses)
        );
    }

    private List<PreparedResult> prepareResults(
            QuizAttemptRequest request,
            List<QuizScoringQuestionRow> questions,
            List<QuizScoringOptionRow> options
    ) {
        if (request == null || request.answers() == null) {
            throw validationFailed();
        }
        Map<Long, QuizAnswerRequest> answersByQuestion = new LinkedHashMap<>();
        for (QuizAnswerRequest answer : request.answers()) {
            if (answer == null || answersByQuestion.putIfAbsent(
                    answer.questionId(),
                    answer
            ) != null) {
                throw validationFailed();
            }
        }
        Set<Long> expectedQuestionIds = questions.stream()
                .map(QuizScoringQuestionRow::questionId)
                .collect(java.util.stream.Collectors.toSet());
        if (!answersByQuestion.keySet().equals(expectedQuestionIds)) {
            throw validationFailed();
        }

        Map<Long, List<ScoringOption>> optionsByQuestion = new HashMap<>();
        for (QuizScoringOptionRow option : options) {
            optionsByQuestion.computeIfAbsent(
                    option.questionId(),
                    ignored -> new ArrayList<>()
            ).add(new ScoringOption(
                    option.optionId(),
                    option.correct(),
                    option.sortOrder()
            ));
        }

        return questions.stream()
                .map(question -> new PreparedResult(
                        question,
                        scorer.score(
                                question.type(),
                                answersByQuestion.get(question.questionId())
                                        .selectedOptionIds(),
                                optionsByQuestion.getOrDefault(
                                        question.questionId(),
                                        List.of()
                                )
                        )
                ))
                .toList();
    }

    private QuizAttemptEntity attempt(
            long userId,
            long quizId,
            int totalQuestions,
            int correctCount,
            int scorePercent,
            LocalDateTime submittedAt
    ) {
        QuizAttemptEntity attempt = new QuizAttemptEntity();
        attempt.setUserId(userId);
        attempt.setQuizId(quizId);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCorrectCount(correctCount);
        attempt.setScorePercent(scorePercent);
        attempt.setSubmittedAt(submittedAt);
        return attempt;
    }

    private QuizAnswerEntity answer(long attemptId, long questionId, boolean correct) {
        QuizAnswerEntity answer = new QuizAnswerEntity();
        answer.setAttemptId(attemptId);
        answer.setQuestionId(questionId);
        answer.setCorrect(correct);
        return answer;
    }

    private QuizAnswerOptionEntity selectedOption(long answerId, long optionId) {
        QuizAnswerOptionEntity selectedOption = new QuizAnswerOptionEntity();
        selectedOption.setQuizAnswerId(answerId);
        selectedOption.setOptionId(optionId);
        return selectedOption;
    }

    private BusinessException validationFailed() {
        return new BusinessException(ErrorCode.VALIDATION_FAILED);
    }

    private BusinessException resourceNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private record PreparedResult(
            QuizScoringQuestionRow question,
            ObjectiveScore score
    ) {
    }
}
