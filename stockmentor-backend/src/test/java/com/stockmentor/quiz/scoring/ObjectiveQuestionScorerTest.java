package com.stockmentor.quiz.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.domain.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ObjectiveQuestionScorerTest {

    private final ObjectiveQuestionScorer scorer = new ObjectiveQuestionScorer();

    @Test
    void singleChoiceScoresOnlyTheExactCorrectOption() {
        List<ScoringOption> options = singleOptions();

        assertThat(scorer.score(QuestionType.SINGLE_CHOICE, List.of(11L), options).correct())
                .isTrue();
        assertThat(scorer.score(QuestionType.SINGLE_CHOICE, List.of(12L), options).correct())
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class, names = {"SINGLE_CHOICE", "TRUE_FALSE"})
    void singleAndTrueFalseRequireExactlyOneSelection(QuestionType type) {
        assertValidationFailure(() -> scorer.score(type, List.of(), singleOptions()));
        assertValidationFailure(() -> scorer.score(type, List.of(11L, 12L), singleOptions()));
    }

    @Test
    void trueFalseUsesTheSameExactSetRule() {
        List<ScoringOption> options = List.of(
                new ScoringOption(21L, false, 1),
                new ScoringOption(22L, true, 2)
        );

        assertThat(scorer.score(QuestionType.TRUE_FALSE, List.of(22L), options).correct())
                .isTrue();
        assertThat(scorer.score(QuestionType.TRUE_FALSE, List.of(21L), options).correct())
                .isFalse();
    }

    @Test
    void multipleChoiceRequiresAnExactSetRegardlessOfInputOrder() {
        List<ScoringOption> options = multipleOptions();

        ObjectiveScore ordered = scorer.score(
                QuestionType.MULTIPLE_CHOICE,
                List.of(31L, 33L),
                options
        );
        ObjectiveScore reversed = scorer.score(
                QuestionType.MULTIPLE_CHOICE,
                List.of(33L, 31L),
                options
        );

        assertThat(ordered.correct()).isTrue();
        assertThat(reversed.correct()).isTrue();
        assertThat(reversed.selectedOptionIds()).containsExactly(31L, 33L);
        assertThat(reversed.correctOptionIds()).containsExactly(31L, 33L);
    }

    @Test
    void multipleChoiceDoesNotAwardPartialCredit() {
        List<ScoringOption> options = multipleOptions();

        assertThat(scoreMultiple(List.of(31L), options).correct()).isFalse();
        assertThat(scoreMultiple(List.of(31L, 32L, 33L), options).correct()).isFalse();
        assertThat(scoreMultiple(List.of(32L), options).correct()).isFalse();
    }

    @Test
    void multipleChoiceRejectsAnEmptySelection() {
        assertValidationFailure(() -> scoreMultiple(List.of(), multipleOptions()));
    }

    @Test
    void duplicateOrForeignOptionIdsAreInvalidInsteadOfSilentlyNormalized() {
        assertValidationFailure(() -> scoreMultiple(
                List.of(31L, 31L),
                multipleOptions()
        ));
        assertValidationFailure(() -> scoreMultiple(
                List.of(31L, 999L),
                multipleOptions()
        ));
    }

    private ObjectiveScore scoreMultiple(
            List<Long> selectedOptionIds,
            List<ScoringOption> options
    ) {
        return scorer.score(
                QuestionType.MULTIPLE_CHOICE,
                selectedOptionIds,
                options
        );
    }

    private List<ScoringOption> singleOptions() {
        return List.of(
                new ScoringOption(11L, true, 1),
                new ScoringOption(12L, false, 2)
        );
    }

    private List<ScoringOption> multipleOptions() {
        return List.of(
                new ScoringOption(31L, true, 1),
                new ScoringOption(32L, false, 2),
                new ScoringOption(33L, true, 3)
        );
    }

    private void assertValidationFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
