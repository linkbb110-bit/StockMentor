package com.stockmentor.quiz.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class V4QuizMigrationContractTest {

    private static final String MIGRATION =
            "db/migration/V4__create_quiz_wrong_question_tables.sql";

    @Test
    void definesTheApprovedEightTableSchemaAndConcurrencyConstraints() throws IOException {
        String sql = migrationSql();

        assertThat(createdTables(sql)).containsExactly(
                "question",
                "question_option",
                "quiz",
                "quiz_question",
                "quiz_attempt",
                "quiz_answer",
                "quiz_answer_option",
                "wrong_question"
        );
        assertThat(sql)
                .contains("UNIQUE KEY uk_quiz_lesson (lesson_id)")
                .contains("UNIQUE KEY uk_question_option_question_key (question_id, option_key)")
                .contains("UNIQUE KEY uk_question_option_question_sort (question_id, sort_order)")
                .contains("UNIQUE KEY uk_quiz_question_quiz_question (quiz_id, question_id)")
                .contains("UNIQUE KEY uk_quiz_question_quiz_sort (quiz_id, sort_order)")
                .contains("UNIQUE KEY uk_quiz_question_question (question_id)")
                .contains("UNIQUE KEY uk_quiz_answer_attempt_question (attempt_id, question_id)")
                .contains("PRIMARY KEY (quiz_answer_id, option_id)")
                .contains("UNIQUE KEY uk_wrong_question_user_question (user_id, question_id)")
                .contains("KEY idx_quiz_attempt_user_submitted (user_id, submitted_at, id)")
                .contains("KEY idx_quiz_attempt_quiz (quiz_id, id)")
                .contains("KEY idx_wrong_question_user_pending (user_id, status, last_wrong_at, id)")
                .contains("KEY idx_wrong_question_user_mastered (user_id, status, mastered_at, id)")
                .contains("published TINYINT NOT NULL DEFAULT 0")
                .doesNotContain(" deleted ")
                .doesNotContain(" JSON ");
    }

    @Test
    void seedsOneQuizPerLessonAndTwoOriginalQuestionsPerQuiz() throws IOException {
        String sql = migrationSql();

        assertThat(valuesRowCount(insertStatement(sql, "quiz"))).isEqualTo(20);
        assertThat(valuesRowCount(insertStatement(sql, "question"))).isEqualTo(40);
        assertThat(valuesRowCount(insertStatement(sql, "quiz_question"))).isEqualTo(40);
        assertThat(valuesRowCount(insertStatement(sql, "question_option"))).isGreaterThanOrEqualTo(80);
        assertThat(insertStatement(sql, "question"))
                .contains("'SINGLE_CHOICE'")
                .contains("'MULTIPLE_CHOICE'")
                .contains("'TRUE_FALSE'");
        assertThat(insertStatement(sql, "question_option"))
                .contains("'TRUE'")
                .contains("'FALSE'");
    }

    private String migrationSql() throws IOException {
        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(MIGRATION)) {
            assertThat(stream).as("V4 migration resource").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> createdTables(String sql) {
        Matcher matcher = Pattern.compile("(?im)^CREATE TABLE ([a-z_]+) \\(").matcher(sql);
        return matcher.results().map(result -> result.group(1)).toList();
    }

    private String insertStatement(String sql, String table) {
        Matcher matcher = Pattern.compile(
                "(?is)INSERT INTO " + Pattern.quote(table) + " \\(.*?;"
        ).matcher(sql);
        assertThat(matcher.find()).as("seed statement for " + table).isTrue();
        return matcher.group();
    }

    private long valuesRowCount(String statement) {
        int valuesIndex = statement.toUpperCase().indexOf("VALUES");
        assertThat(valuesIndex).isGreaterThanOrEqualTo(0);
        String values = statement.substring(valuesIndex + "VALUES".length());
        return Pattern.compile("(?m)^\\s*\\(").matcher(values).results().count();
    }
}
