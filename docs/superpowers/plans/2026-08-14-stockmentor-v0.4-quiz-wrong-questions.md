# StockMentor V0.4 Quiz and Wrong-Question Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development`
> with at most one primary implementation Agent per main Task, or use
> `superpowers:executing-plans` for inline execution. Do not create per-Task spec
> reviewers, quality reviewers, or rereviewers.

**Goal:** Deliver an objective Quiz and wrong-question review loop from each published
Lesson through backend scoring, immutable Attempts, persisted PENDING/MASTERED state,
and refresh-safe Vue review pages.

**Architecture:** Add a focused `com.stockmentor.quiz` module to the existing modular
monolith, preserving Controller → Service → Repository → Mapper boundaries. Public
read projections exclude answer material; private transactional Services share one
exact-set scorer and derive user ownership only from `AuthenticatedUser`. The Vue
feature reuses the existing Axios/AuthStore/router infrastructure and component-local state.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Security, MyBatis-Plus, Flyway,
MySQL 8.4, Vue 3, TypeScript, Vue Router, Axios, Vitest.

**Approved design:**
`docs/superpowers/specs/2026-08-14-stockmentor-v0.4-quiz-wrong-questions-design.md`

## Global Constraints

- Implement exactly the four main Tasks below. Do not add AI/SHORT_ANSWER, Attempt
  History, CMS/Admin, Redis/MQ, Dashboard aggregation, company analysis, virtual
  investment, V0.5+, or dependency upgrades.
- Keep every API under `/api/v1` and every body inside `ApiResponse<T>`; use real HTTP
  201 for new Attempt creation, 200 for reads/reviews, 400 for invalid submission,
  401 for authentication failure, and 404 for unavailable published resources.
- Public `GET /api/v1/lessons/{lessonId}/quiz` must remain anonymous even with an
  invalid/expired Bearer Token. Every `/api/v1/me/**` path stays authenticated.
- Obtain `userId` only from `AuthenticatedUser.userId()`; requests never accept it,
  and every private query/update contains the current user ID.
- Preserve V1–V3. V4 is the sole migration and must run V1 → V2 → V3 → V4 on an
  empty MySQL 8.4 database.
- Public Quiz and wrong-question list responses never contain `isCorrect`, correct
  Option IDs, or explanations. Those fields appear only after authenticated answers.
- Use exact-set scoring for all objective types; input order is irrelevant and
  duplicate Option IDs are invalid.
- Use TDD for Tasks 1–3: focused failing test, recorded red evidence, minimal
  implementation, focused pass, necessary regression, then one scoped commit.
- Run complete Maven/npm suites and real MySQL/HTTP/browser verification only once
  inside Task 4. Do not replace actual evidence with inferred success.
- Do not log passwords, complete Tokens, credentials, correct-answer sets, or full
  answer submissions. Do not commit local environment files, logs, or test data.

## Cross-Task Contracts

Task 1 produces the public response contract:

```java
record PublicQuizResponse(long id, long lessonId, String lessonTitle,
    long courseId, String courseTitle, long chapterId, String chapterTitle,
    String title, String summary, List<QuizQuestionResponse> questions) {}
record QuizQuestionResponse(long questionId, QuestionType type, String stem,
    List<QuizOptionResponse> options) {}
record QuizOptionResponse(long optionId, String optionKey, String content) {}
```

Task 2 extends the repository internally and produces private contracts:

```java
QuizAttemptResponse QuizAttemptService.submit(long userId, long quizId,
    QuizAttemptRequest request);
List<WrongQuestionResponse> WrongQuestionService.list(long userId,
    WrongQuestionStatus status);
WrongQuestionReviewResponse WrongQuestionService.review(long userId,
    long questionId, WrongQuestionAnswerRequest request);
```

```text
GET  /api/v1/lessons/{lessonId}/quiz
POST /api/v1/me/quizzes/{quizId}/attempts
GET  /api/v1/me/wrong-questions?status=PENDING|MASTERED
POST /api/v1/me/wrong-questions/{questionId}/answer
```

Task 3 consumes these JSON fields verbatim; it does not infer answers, scores, user IDs,
or mastery. Task 4 changes no contract unless a verified integration defect requires a
minimal correction before the final Task commit.

---

### Task 1: Database + Public Quiz Read Model

**Deliverable:** V4 defines all eight V0.4 tables and an original 20 Quiz / 40 Question
seed; anonymous clients can read an ordered published Quiz without answer leakage or
Question-level N+1 queries.

**Files:**

- Create: `stockmentor-backend/src/main/resources/db/migration/V4__create_quiz_wrong_question_tables.sql`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/domain/QuestionType.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/entity/QuestionEntity.java`,
  `QuestionOptionEntity.java`, `QuizEntity.java`, `QuizQuestionEntity.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/mapper/QuestionMapper.java`,
  `QuestionOptionMapper.java`, `QuizMapper.java`, `QuizQuestionMapper.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/repository/QuizRepository.java`,
  `MyBatisQuizRepository.java`, `PublishedQuizRow.java`, `PublicQuizQuestionRow.java`,
  `PublicQuizOptionRow.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/service/QuizQueryService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/controller/QuizController.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/vo/PublicQuizResponse.java`,
  `QuizQuestionResponse.java`, `QuizOptionResponse.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/PublicCourseGetRequestMatcher.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/migration/V4QuizMigrationContractTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/repository/MyBatisQuizRepositoryTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/service/QuizQueryServiceTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/controller/QuizControllerTest.java`
- Modify test: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/PublicCourseGetRequestMatcherTest.java`,
  `SecurityConfigTest.java`, `JwtAuthenticationFilterTest.java`

**Consumes:** V3 `course/chapter/lesson/sys_user`, existing `ApiResponse`,
`RESOURCE_NOT_FOUND`, shared public matcher, Spring Security filter chain, and
MyBatis entity/repository conventions.

**Produces:**

```java
Optional<PublishedQuizRow> QuizRepository.findPublishedByLessonId(long lessonId);
List<PublicQuizQuestionRow> QuizRepository.findPublishedQuestions(long quizId);
List<PublicQuizOptionRow> QuizRepository.findPublishedOptions(List<Long> questionIds);
PublicQuizResponse QuizQueryService.getPublishedQuiz(long lessonId);
```

`PublishedQuizRow` contains Quiz and Lesson/Chapter/Course context but no answer data.
Public Question rows exclude `explanation`; public Option rows exclude `isCorrect`.
Questions sort by `quiz_question.sort_order, question.id`; Options sort by
`question_option.sort_order, question_option.id`.

- [ ] **Step 1: Write focused failing database/read/security tests.** Assert V4 names
  eight tables, all specified FKs/checks/indexes, `question_option.published`,
  unique Quiz-per-Lesson and Question-per-Quiz mapping, and seed IDs/count structure.
  Repository/Service tests assert the three fixed batch reads, stable order, empty
  published-question 404, and Course/Lesson/Quiz/Question/Option publication filters.
  Strict Controller JSON must prove `isCorrect`, `correctOptionIds`, and `explanation`
  are absent. Matcher/filter-chain tests must prove only GET Lesson Quiz is added;
  invalid/expired Token remains public and all non-GET/private shapes remain strict.
- [ ] **Step 2: Run red tests and record evidence.** From repository root run:

  ```powershell
  mvn.cmd -f stockmentor-backend\pom.xml -Dtest=V4QuizMigrationContractTest,MyBatisQuizRepositoryTest,QuizQueryServiceTest,QuizControllerTest,PublicCourseGetRequestMatcherTest,SecurityConfigTest,JwtAuthenticationFilterTest test
  ```

  Expect compilation/assertion failure because V4 and Quiz types do not exist; record
  the exit code and one representative failure.
- [ ] **Step 3: Implement V4 schema and original seed.** Create `question`,
  `question_option`, `quiz`, `quiz_question`, `quiz_attempt`, `quiz_answer`,
  `quiz_answer_option`, and `wrong_question` exactly as the Design specifies. Seed
  one Quiz per Lesson and two original questions per Quiz using the Design's 20-row
  learning-objective/type matrix. Do not modify older migrations or add runtime seed.
- [ ] **Step 4: Implement the minimal public read model.** Mapper queries must require
  published Course/Lesson/Quiz, then batch published Questions and Options. Service
  assembles immutable response records by IDs; it returns 404 for unavailable or
  zero-question Quiz and never maps internal correctness/analysis fields.
- [ ] **Step 5: Extend security through one matcher source.** Add only regex shape
  `^/api/v1/lessons/[^/]+/quiz$` for GET. Keep the same matcher bean in `permitAll`
  and `JwtAuthenticationFilter.shouldNotFilter`; do not change any `/api/v1/me/**`
  rule or CORS wildcard behavior.
- [ ] **Step 6: Run focused tests to green.** Rerun Step 2 exactly once after fixes and
  record test count/exit code. Run `CourseControllerTest,LessonControllerTest` once as
  the necessary public-course regression.
- [ ] **Step 7: Inspect scope and commit.** Run `git diff --check`, confirm only V4,
  public Quiz read/security, and direct tests are present, then commit:

  ```powershell
  git commit -m "feat: add V0.4 public quiz read model"
  ```

---

### Task 2: Scoring + Quiz Attempts + Wrong Question State Machine

**Deliverable:** Authenticated users can submit complete objective Quizzes and review
only their own wrong questions; persistence is transactional, scoring is shared, and
all six wrong-question transitions are correct.

**Files:**

- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/domain/WrongQuestionStatus.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/entity/QuizAttemptEntity.java`,
  `QuizAnswerEntity.java`, `QuizAnswerOptionEntity.java`, `WrongQuestionEntity.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/mapper/QuizAttemptMapper.java`,
  `QuizAnswerMapper.java`, `QuizAnswerOptionMapper.java`, `WrongQuestionMapper.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/repository/QuizAttemptRepository.java`,
  `MyBatisQuizAttemptRepository.java`, `WrongQuestionRepository.java`,
  `MyBatisWrongQuestionRepository.java`, `QuizScoringQuestionRow.java`,
  `QuizScoringOptionRow.java`, `WrongQuestionRow.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/quiz/repository/QuizRepository.java`,
  `MyBatisQuizRepository.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/scoring/ObjectiveQuestionScorer.java`,
  `ScoringOption.java`, `ObjectiveScore.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/dto/QuizAttemptRequest.java`,
  `QuizAnswerRequest.java`, `WrongQuestionAnswerRequest.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/service/QuizAttemptService.java`,
  `WrongQuestionService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/controller/QuizAttemptController.java`,
  `WrongQuestionController.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/quiz/vo/QuizAttemptResponse.java`,
  `QuizQuestionResultResponse.java`, `WrongQuestionResponse.java`,
  `WrongQuestionReviewResponse.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/scoring/ObjectiveQuestionScorerTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/repository/MyBatisQuizAttemptRepositoryTest.java`,
  `MyBatisWrongQuestionRepositoryTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/service/QuizAttemptServiceTest.java`,
  `QuizAttemptServiceTransactionTest.java`, `WrongQuestionServiceTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/quiz/controller/QuizAttemptControllerTest.java`,
  `WrongQuestionControllerTest.java`
- Modify test: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/SecurityConfigTest.java`

**Consumes:** Task 1 entities/read contracts and V4 constraints. Public projections
stay unchanged; private scoring rows add `explanation` and Option `isCorrect` only
inside the backend.

**Produces:**

```java
ObjectiveScore ObjectiveQuestionScorer.score(QuestionType type,
    List<Long> selectedOptionIds, List<ScoringOption> options);
record ObjectiveScore(boolean correct, List<Long> selectedOptionIds,
    List<Long> correctOptionIds) {}
```

```java
record QuizAttemptRequest(List<QuizAnswerRequest> answers) {}
record QuizAnswerRequest(long questionId, List<Long> selectedOptionIds) {}
record WrongQuestionAnswerRequest(List<Long> selectedOptionIds) {}
```

Attempt result ordering follows Quiz question order. Wrong-list rows include Lesson
context/options but exclude correctness/explanation. Review response includes the
backend result, new status/error count, correct Option IDs, and explanation.

- [ ] **Step 1: Write scorer and validation red tests.** Cover SINGLE and TRUE_FALSE
  correct/incorrect/exactly-one validation; MULTIPLE exact set, reversed order,
  missing/extra option; empty selection, duplicate Option ID, Option from another
  Question, and unpublished Option rejection. Service tests cover missing/extra/
  duplicate Question, unpublished chain, and zero-question 404 before any insert.
- [ ] **Step 2: Write transaction/state/isolation red tests.** Cover immutable Attempt,
  Answer and normalized selection persistence, floor/100 scoring, a second legal POST
  creating a second Attempt, all six state transitions, never-wrong correct no-op,
  list ordering/publication filters, A/B isolation, and anonymous private 401.
  `QuizAttemptServiceTransactionTest` must invoke the Service through Spring's
  transaction interceptor with a recording transaction manager, force an
  AnswerOption/WrongQuestion persistence exception, and assert rollback plus exception
  propagation rather than swallowing it.
- [ ] **Step 3: Run red tests and record evidence.** Run:

  ```powershell
  mvn.cmd -f stockmentor-backend\pom.xml -Dtest=ObjectiveQuestionScorerTest,MyBatisQuizAttemptRepositoryTest,MyBatisWrongQuestionRepositoryTest,QuizAttemptServiceTest,QuizAttemptServiceTransactionTest,WrongQuestionServiceTest,QuizAttemptControllerTest,WrongQuestionControllerTest,SecurityConfigTest test
  ```

  Expect missing scorer/private API compilation failures; record exit code and a
  representative failure.
- [ ] **Step 4: Implement shared scoring and complete pre-write validation.** Load the
  published Quiz snapshot in three batch queries, validate exact Question membership,
  then score every answer before inserting anything. Sort selected/correct IDs by
  Option order. Use integer floor arithmetic and force all-correct to 100.
- [ ] **Step 5: Implement transactional persistence.** Mark `submit` transactional;
  insert one Attempt and ordered Answers/AnswerOptions. `recordWrong` uses atomic
  MySQL upsert on `(user_id, question_id)` with `error_count + 1`; `recordCorrect`
  updates only an existing current-user row and preserves an existing mastered time.
  Read the resulting state by both user ID and Question ID before returning.
- [ ] **Step 6: Implement wrong-list/review publication and isolation.** List query
  joins through Question→Quiz→Lesson→Course and applies current user, requested status,
  and every published flag before batch-loading public Options. Review requires the
  current user's visible record, calls the same scorer, applies the Service-selected
  transition, and never inserts an Attempt.
- [ ] **Step 7: Implement private Controllers.** Read `userId` only from
  `@AuthenticationPrincipal`; apply `@Valid`; return 201 for Attempt and 200 for list/
  review. Invalid status/cardinality uses `VALIDATION_FAILED`; unavailable or
  other-user records use `RESOURCE_NOT_FOUND` without existence leakage.
- [ ] **Step 8: Run focused tests and regression to green.** Rerun Step 3, then run
  existing auth/current-user/progress security tests once. Record actual counts and
  exit codes; do not run the complete backend suite.
- [ ] **Step 9: Inspect scope and commit.** Run `git diff --check`, confirm no frontend,
  Dashboard, AI, or schema changes beyond V4, then commit:

  ```powershell
  git commit -m "feat: add V0.4 quiz attempts and wrong questions"
  ```

---

### Task 3: Frontend Quiz + Wrong Question Experience

**Deliverable:** Anonymous users can read three objective Question types; authenticated
users can submit without client-side scoring and review persisted PENDING/MASTERED
wrong questions through the existing session and 401 flow.

**Files:**

- Create: `stockmentor-frontend/src/features/quiz/types/quiz.ts`
- Create: `stockmentor-frontend/src/features/quiz/api/quizApi.ts`, `quizApi.spec.ts`
- Create: `stockmentor-frontend/src/features/quiz/components/QuestionOptions.vue`,
  `QuestionOptions.spec.ts`
- Create: `stockmentor-frontend/src/features/quiz/views/QuizView.vue`, `QuizView.spec.ts`
- Create: `stockmentor-frontend/src/features/quiz/views/WrongQuestionsView.vue`,
  `WrongQuestionsView.spec.ts`
- Modify: `stockmentor-frontend/src/features/course/views/LessonView.vue`,
  `LessonView.spec.ts`
- Modify: `stockmentor-frontend/src/router/index.ts`, `routerGuards.spec.ts`

**Consumes:** Existing Axios instance with `/api/v1` baseURL and global 401 ownership
handling; `AuthStore.isAuthenticated`; Task 1 public and Task 2 private JSON contracts.

**Produces:**

```ts
getQuiz(lessonId: number): Promise<AxiosResponse<ApiResponse<PublicQuiz>>>
submitQuiz(quizId: number, payload: QuizAttemptPayload):
  Promise<AxiosResponse<ApiResponse<QuizAttemptResult>>>
listWrongQuestions(status: WrongQuestionStatus):
  Promise<AxiosResponse<ApiResponse<WrongQuestion[]>>>
reviewWrongQuestion(questionId: number, payload: WrongQuestionAnswerPayload):
  Promise<AxiosResponse<ApiResponse<WrongQuestionReviewResult>>>
```

`QuestionOptions` receives Question type/options plus selected IDs and emits selected
IDs; radio is used for SINGLE/TRUE_FALSE and checkbox for MULTIPLE. It receives no
correctness data.

- [ ] **Step 1: Write API/component/route red tests.** Assert exact relative Axios
  paths/payloads, radio/checkbox rendering, stable Option labels, public Quiz route,
  protected wrong-question route, and existing route permissions. Lesson test asserts
  the new Quiz link uses the current route-param Lesson and survives Lesson 1→2 reuse.
- [ ] **Step 2: Write QuizView red tests.** Cover loading/error, all three types,
  independent selections, anonymous public GET with no private call, login navigation
  on anonymous submit, authenticated payload, submit pending/double-click, API failure
  recovery, backend score/results/explanations, and conditional wrong-question link.
  Assertions must prove no success is shown before the API response and the displayed
  score is the backend value.
- [ ] **Step 3: Write WrongQuestionsView red tests.** Cover default PENDING load,
  MASTERED switch, empty/error/retry, per-item pending, wrong review/errorCount refresh,
  correct review leaving PENDING, mastered-wrong returning to PENDING, API failure,
  and remount refresh from backend state. Assert list payload never expects answer or
  explanation fields.
- [ ] **Step 4: Run red tests and record evidence.** From `stockmentor-frontend` run:

  ```powershell
  npm.cmd run test:unit -- src/features/quiz src/features/course/views/LessonView.spec.ts src/router/routerGuards.spec.ts
  ```

  Expect missing feature/routes and assertions to fail; record exit code and one
  representative failure.
- [ ] **Step 5: Implement typed API and route boundaries.** Define explicit interfaces
  without `any`, use `/lessons/${lessonId}/quiz` and `/me/...` relative URLs, register
  public Quiz and protected wrong-question routes, and keep the existing AuthStore,
  sessionStorage, and interceptor behavior unchanged.
- [ ] **Step 6: Implement QuizView with backend-owned results.** Load only public data
  for anonymous users. Build one answer per Question from local selections, require
  login before any private request, disable pending submission, and render only the
  returned Attempt/result. Failure restores controls and does not fabricate correctness.
- [ ] **Step 7: Implement WrongQuestionsView with backend-owned state.** Load the active
  status from API, submit only selected IDs, show returned feedback, then reload that
  status so transitions and errorCount come from the server. Keep pending/error state
  isolated per Question and do not create a Pinia Quiz cache.
- [ ] **Step 8: Implement the Lesson entry and run focused green tests.** Add one clear
  “课后测验” RouterLink without changing Lesson completion. Rerun Step 4, then run:

  ```powershell
  npm.cmd run type-check
  ```

  Record file/test counts and exit codes. Do not run the full frontend suite/build.
- [ ] **Step 9: Inspect scope and commit.** Run `git diff --check`, confirm no Dashboard,
  dependency, backend, AI, or V0.5+ changes, then commit:

  ```powershell
  git commit -m "feat: add V0.4 quiz learning experience"
  ```

---

### Task 4: Integration + Docs + Final Verification

**Deliverable:** The complete Lesson→Quiz→Result→Wrong Questions→MASTERED loop has one
recorded full verification against clean dependencies and isolated MySQL 8.4, with
concise README/changelog evidence and no residual test environment.

**Files:**

- Modify: `README.md`
- Create: `docs/changelog/2026-08-14-v0.4-quiz-wrong-questions.md`
- Verify without planned behavior changes: all Task 1–3 source/test files and existing
  auth/course/router regressions named above

**Consumes:** All Task 1–3 commits and existing V0.1–V0.3 features.

**Produces:** V0.4 status/docs, one Task 4 commit, complete command/runtime evidence,
and a clean branch ready for the separately authorized whole-branch review.

- [ ] **Step 1: Run focused integration regression before documentation.** Run the
  Task 1–3 focused commands once, including public invalid-Token, private anonymous
  401, scorer/state/isolation, QuizView/WrongQuestionsView, Lesson and router tests.
  If a real failure occurs, preserve evidence, locate the root cause, make only the
  smallest correction in its owning Task file, and rerun only the failed focus.
- [ ] **Step 2: Run the single complete backend verification.** Use Java 17 and run
  once each from repository root:

  ```powershell
  mvn.cmd -f stockmentor-backend\pom.xml clean test
  mvn.cmd -f stockmentor-backend\pom.xml clean package
  ```

  Record each exit code, test count, failures/errors, package result, and warnings.
- [ ] **Step 3: Run the single complete frontend verification.** Run once each:

  ```powershell
  Set-Location stockmentor-frontend
  npm.cmd ci
  npm.cmd run test:unit
  npm.cmd run type-check
  npm.cmd run build
  Set-Location ..
  ```

  Record exit codes, test files/tests, type-check/build result, audit warning, and
  existing chunk warning without upgrading dependencies.
- [ ] **Step 4: Verify clean MySQL V1→V4.** Start a disposable isolated MySQL 8.4
  instance/schema outside personal data, migrate a genuinely empty database, and
  verify Flyway versions 1/2/3/4; all eight new tables; every FK/check/unique/index;
  and counts 1 Course / 10 Chapter / 20 Lesson / 20 Quiz / 40 Question. Verify each
  Lesson has one Quiz, each Quiz has two Questions, and every Question has valid
  published Options/correct cardinality.
- [ ] **Step 5: Verify real HTTP and transaction/security behavior.** Start the backend
  against the disposable database and verify public Quiz with no/invalid/expired Token;
  no answer/analysis leak; login; complete valid submission; repeated POST creates a
  distinct Attempt; PENDING→MASTERED→PENDING transitions; A/B list/review isolation;
  private anonymous 401; unpublished Course/Lesson/Quiz/Question 404. In the isolated
  database only, install a temporary failing trigger on a late submission write,
  submit once, then prove zero partial Attempt/Answer/AnswerOption/WrongQuestion rows;
  remove the trigger immediately.
- [ ] **Step 6: Verify the real browser loop.** In a clean in-app browser session run:

  ```text
  Lesson → Quiz → intentionally wrong submission → backend Result
  → Wrong Questions/PENDING → answer correctly → MASTERED
  → refresh → still MASTERED
  ```

  Also confirm anonymous Quiz never calls private APIs, pending disables double-click,
  login flow works, three Question types render, and Lesson completion is unchanged.
- [ ] **Step 7: Security and cleanup checks.** Scan tracked changes/logs for passwords,
  complete Tokens, JWT/database secrets, correct-answer logging, and local environment
  files. Stop frontend/backend/MySQL test processes; prove test ports have no listeners;
  remove the disposable schema/data directory, trigger, logs, and control files while
  leaving the installed personal MySQL service/data untouched.
- [ ] **Step 8: Write exact documentation evidence.** Update README implemented scope,
  routes/APIs and verified counts. Write the changelog with Asia/Shanghai completion
  time (`YYYY-MM-DD HH:mm:ss`), changed files, behavior, actual commands/exit codes,
  test/build/MySQL/HTTP/browser evidence, errors/root causes, warnings, cleanup,
  unfinished items, and the explicit V0.5 boundary. Do not claim unrun verification.
- [ ] **Step 9: Inspect and commit only Task 4.** Run `git diff --check`, inspect staged
  files for only integration fixes (if evidence required them) plus README/changelog,
  verify no V0.5+/dependency upgrade, then commit:

  ```powershell
  git commit -m "feat: complete V0.4 quiz and wrong-question flow"
  ```

## Completion Gate

After Task 4 is committed, run exactly one independent whole-branch Review against the
approved Design, focused on answer leakage, JWT matcher precision, current-user SQL,
transaction rollback, wrong-state concurrency, publication filters, exact-set scoring,
frontend source-of-truth behavior, and meaningful test coverage.

If Critical/Important findings are zero, perform one separate fresh release verification
before any authorized push/PR. If such findings exist, fix only validated findings, run
one scoped re-review, then fresh verification. Do not push, create a PR, or start V0.5
without the corresponding execution instruction.
