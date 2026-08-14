package com.stockmentor.quiz.scoring;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.domain.QuestionType;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ObjectiveQuestionScorer {

    public ObjectiveScore score(
            QuestionType type,
            List<Long> selectedOptionIds,
            List<ScoringOption> options
    ) {
        if (type == null || selectedOptionIds == null || options == null) {
            throw validationFailed();
        }

        validateCardinality(type, selectedOptionIds.size());
        Set<Long> selectedSet = new HashSet<>(selectedOptionIds);
        if (selectedSet.size() != selectedOptionIds.size()) {
            throw validationFailed();
        }

        List<ScoringOption> orderedOptions = options.stream()
                .sorted(Comparator.comparingInt(ScoringOption::sortOrder)
                        .thenComparingLong(ScoringOption::optionId))
                .toList();
        Set<Long> availableOptionIds = orderedOptions.stream()
                .map(ScoringOption::optionId)
                .collect(java.util.stream.Collectors.toSet());
        if (!availableOptionIds.containsAll(selectedSet)) {
            throw validationFailed();
        }

        List<Long> orderedSelectedIds = orderedOptions.stream()
                .map(ScoringOption::optionId)
                .filter(selectedSet::contains)
                .toList();
        List<Long> correctOptionIds = orderedOptions.stream()
                .filter(ScoringOption::correct)
                .map(ScoringOption::optionId)
                .toList();

        return new ObjectiveScore(
                new HashSet<>(correctOptionIds).equals(selectedSet),
                List.copyOf(orderedSelectedIds),
                List.copyOf(correctOptionIds)
        );
    }

    private void validateCardinality(QuestionType type, int selectedCount) {
        switch (Objects.requireNonNull(type)) {
            case SINGLE_CHOICE, TRUE_FALSE -> {
                if (selectedCount != 1) {
                    throw validationFailed();
                }
            }
            case MULTIPLE_CHOICE -> {
                if (selectedCount < 1) {
                    throw validationFailed();
                }
            }
        }
    }

    private BusinessException validationFailed() {
        return new BusinessException(ErrorCode.VALIDATION_FAILED);
    }
}
