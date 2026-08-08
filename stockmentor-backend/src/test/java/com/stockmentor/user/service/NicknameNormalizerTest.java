package com.stockmentor.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NicknameNormalizerTest {

    private NicknameNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new NicknameNormalizer();
    }

    @Test
    void trimsAValidChineseNickname() {
        assertThat(normalizer.normalize("  长期学习者  "))
                .isEqualTo("长期学习者");
    }

    @Test
    void acceptsAValueAtTheTwoCharacterLowerBoundary() {
        assertThat(normalizer.normalize(" 学习 "))
                .isEqualTo("学习");
    }

    @Test
    void acceptsAValueAtTheTwentyCharacterUpperBoundaryAfterTrimming() {
        String nicknameAtBoundary = "学".repeat(20);

        assertThat(normalizer.normalize("  " + nicknameAtBoundary + "  "))
                .isEqualTo(nicknameAtBoundary);
    }

    @Test
    void rejectsASingleSupplementaryHanCodePointAsOneCharacter() {
        assertNicknameFailure(supplementaryHanNickname(1));
    }

    @Test
    void acceptsTwentySupplementaryHanCodePointsAfterTrimming() {
        String nicknameAtBoundary = supplementaryHanNickname(20);

        assertThat(normalizer.normalize("  " + nicknameAtBoundary + "  "))
                .isEqualTo(nicknameAtBoundary);
    }

    @Test
    void rejectsTwentyOneSupplementaryHanCodePoints() {
        assertNicknameFailure(supplementaryHanNickname(21));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "中文 Learner_01-长期",
        "学习 投资",
        "user_name",
        "learner-01"
    })
    void acceptsApprovedCharacters(String nickname) {
        assertThat(normalizer.normalize(nickname)).isEqualTo(nickname);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "   ",
        "学",
        "学学学学学学学学学学学学学学学学学学学学学",
        "学习者!",
        "学习\t投资",
        "昵称🙂"
    })
    void rejectsInvalidNormalizedNicknamesWithUserNicknameInvalid(String rawNickname) {
        assertNicknameFailure(rawNickname);
    }

    @Test
    void rejectsNullNicknameWithUserNicknameInvalid() {
        assertNicknameFailure(null);
    }

    private String supplementaryHanNickname(int codePointCount) {
        return new String(Character.toChars(0x20000)).repeat(codePointCount);
    }

    private void assertNicknameFailure(String rawNickname) {
        assertThatThrownBy(() -> normalizer.normalize(rawNickname))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_NICKNAME_INVALID));
    }
}
