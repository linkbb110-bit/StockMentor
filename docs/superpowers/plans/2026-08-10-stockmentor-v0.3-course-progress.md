# StockMentor V0.3 Course Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `superpowers:subagent-driven-development`
> with at most one primary implementation Agent per major Task, or use
> `superpowers:executing-plans` for inline execution. Do not create per-Task
> spec reviewers, quality reviewers, or rereviewers.

**Goal:** Deliver the V0.3 learning loop from anonymous course reading through
authenticated lesson completion, persisted progress, and next-lesson navigation.

**Architecture:** Add a `com.stockmentor.course` module inside the existing modular
monolith. Keep Controller → Service → Repository → Mapper boundaries, expose
records rather than entities, and use one shared matcher for public-course security.
The Vue feature uses the existing Axios/AuthStore/sessionStorage infrastructure and
component-local course state.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Security, MyBatis-Plus, Flyway,
MySQL 8, Vue 3, TypeScript, Vue Router, Axios, Vitest, markdown-it.

**Approved design:** `docs/superpowers/specs/2026-08-09-stockmentor-v0.3-course-progress-design.md`

## Global constraints

- Implement exactly the four Tasks below; do not add CMS, admin course APIs,
  `learning_record`, Redis/MQ/Elasticsearch, Refresh Token, new permissions,
  question-bank, AI, or V0.6 aggregation.
- Keep every API under `/api/v1`; all responses remain `ApiResponse<T>`.
- Public course GET requests must work anonymously even when Axios supplies an
  invalid or expired Bearer Token. `/api/v1/me/**` remains strictly authenticated.
- User-private SQL always receives `userId` from `AuthenticatedUser` and includes it
  in progress conditions. Never accept `userId` from path, query, or request body.
- Preserve V1 and V2. V3 is the only new migration and must run on an empty MySQL 8
  database in the sequence V1 → V2 → V3.
- Never log passwords, database credentials, JWT secrets, or complete Tokens.
- Lesson content is original investment education with no security recommendations,
  short-term forecasts, or return promises.
- Use TDD inside each Task: focused failing tests, prove the expected failure,
  minimal implementation, focused pass, required integration evidence, then commit.
- Do not run the complete Maven and npm command set after Tasks 1–3. Run the final
  full suite once in Task 4 after the single final Review.

## Lightweight SDD execution

1. Give a primary Agent only the current Task, this plan's global constraints, the
   approved Design, and the files named by that Task.
2. Use no more than one primary implementation Agent per Task. The same Agent fixes
   its own focused-test failures before the Task commit.
3. Do not dispatch per-Task reviewers. After all four implementations exist, run one
   final Review limited to security boundaries, user isolation, completion idempotency,
   published-data leakage, SQL/N+1, clear correctness bugs, and Design drift.
4. Fix only concrete final-review findings, rerun the affected focused tests, then run
   the Task 4 full verification. Do not replace evidence with inferred success.

---

### Task 1: Database + Course Read Model

**Deliverable:** A fresh MySQL database contains four V0.3 tables and the approved
1 Course / 10 Chapter / 20 Lesson seed; anonymous clients can read only published,
stably ordered course content without Chapter-level N+1 queries.

**Files:**

- Create: `stockmentor-backend/src/main/resources/db/migration/V3__create_course_progress_tables.sql`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/entity/CourseEntity.java`,
  `ChapterEntity.java`, `LessonEntity.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/mapper/CourseMapper.java`,
  `ChapterMapper.java`, `LessonMapper.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/repository/CourseRepository.java`,
  `MyBatisCourseRepository.java`, `PublishedLessonRow.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/service/CourseQueryService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/controller/CourseController.java`,
  `LessonController.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/vo/CourseSummaryResponse.java`,
  `CourseDetailResponse.java`, `ChapterSummaryResponse.java`, `LessonSummaryResponse.java`,
  `LessonDetailResponse.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/PublicCourseGetRequestMatcher.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtAuthenticationFilter.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/course/repository/MyBatisCourseRepositoryTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/course/service/CourseQueryServiceTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/course/controller/CourseControllerTest.java`,
  `LessonControllerTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/PublicCourseGetRequestMatcherTest.java`
- Modify test: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/SecurityConfigTest.java`,
  `JwtAuthenticationFilterTest.java`

**Consumes:** Existing `ApiResponse.success`, `BusinessException(RESOURCE_NOT_FOUND)`,
`GlobalExceptionHandler`, MyBatis entity/repository conventions, and V1/V2 migrations.

**Produces:**
```java
List<CourseSummaryResponse> CourseQueryService.listPublishedCourses();
CourseDetailResponse CourseQueryService.getPublishedCourse(long courseId);
LessonDetailResponse CourseQueryService.getPublishedLesson(long lessonId);
```

```text
GET /api/v1/courses
GET /api/v1/courses/{courseId}
GET /api/v1/lessons/{lessonId}
```

`LessonDetailResponse` exposes `id`, `title`, `summary`, `contentMd`,
`estimatedMinutes`, `courseId`, `courseTitle`, `chapterId`, and `chapterTitle`.
Course detail exposes ordered Chapter records containing ordered published Lesson
summaries. These contracts are consumed by Tasks 2–4.

- [ ] **Step 1: Write focused failing tests.** Cover published filters, stable
  `sort_order, id` ordering, empty Chapter lists, unpublished/not-found 404, strict
  response fields, and exactly three repository calls for course detail. Add matcher
  and filter tests proving only the three public GET shapes skip JWT parsing; invalid
  or expired Tokens continue anonymously, while non-GET and `/api/v1/me/**` do not.
- [ ] **Step 2: Prove the red state.** Run:

  ```powershell
  Set-Location stockmentor-backend
  mvn.cmd -Dtest=MyBatisCourseRepositoryTest,CourseQueryServiceTest,CourseControllerTest,LessonControllerTest,PublicCourseGetRequestMatcherTest,SecurityConfigTest,JwtAuthenticationFilterTest test
  ```

  Expect failure because V3, course types/endpoints, and the shared public matcher do
  not exist; record the exit code and representative failure.
- [ ] **Step 3: Add V3 and the minimal read model.** Create exactly the InnoDB tables
  `course`, `chapter`, `lesson`, and `user_lesson_progress` with approved keys and
  foreign keys; seed one published Course, ten ordered Chapters, and twenty published
  Lessons with approved titles and four short original Markdown sections. Do not
  modify V1/V2 or create other tables.
- [ ] **Step 4: Implement fixed query boundaries.** `CourseRepository` performs one
  published-course query, one ordered-Chapter query, and one batch published-Lesson
  query for a course. Lesson detail uses one join that requires both Course and Lesson
  to be published. Map entities/projections to response records in the Service.
- [ ] **Step 5: Implement one security rule source.** Use
  `PublicCourseGetRequestMatcher` both in `SecurityBaselineConfig` for exact GET
  `permitAll` and in `JwtAuthenticationFilter.shouldNotFilter`; do not skip JWT for
  `/api/v1/me/**` or broader `/api/v1/**` paths.
- [ ] **Step 6: Run focused tests to green.** Rerun the Step 2 command and record its
  actual test count and exit code. Keep existing V0.2 security tests green.
- [ ] **Step 7: Verify the Task against real MySQL.** On a disposable MySQL 8 schema,
  enable Flyway, confirm versions 1/2/3 and counts `1/10/20`, start the backend, then
  call all three GET endpoints without Authorization and with an invalid Bearer Token.
  Confirm HTTP 200 for published rows, 404 for unpublished/missing rows, stable order,
  and use `EXPLAIN` plus the fixed-call service test as N+1 evidence. Clean the schema,
  backend process, port, and temporary logs.
- [ ] **Step 8: Commit only Task 1.** Inspect staged paths and commit with
  `feat: add V0.3 course read model`.

---

### Task 2: Progress

**Deliverable:** Authenticated users can idempotently complete published Lessons and
read isolated persisted progress; duplicate and concurrent PUTs preserve the first
`completed_at`.

**Files:**

- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/entity/UserLessonProgressEntity.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/mapper/LearningProgressMapper.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/repository/LearningProgressRepository.java`,
  `MyBatisLearningProgressRepository.java`, `NextLessonRow.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/service/LearningProgressService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/controller/LearningProgressController.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/course/vo/LessonCompletionResponse.java`,
  `CourseProgressResponse.java`, `NextLessonResponse.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/course/repository/MyBatisLearningProgressRepositoryTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/course/service/LearningProgressServiceTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/course/controller/LearningProgressControllerTest.java`
- Modify test: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/SecurityConfigTest.java`

**Consumes:** Task 1 `CourseRepository` published Course/Lesson checks; existing
`AuthenticatedUser.userId()`, strict `/api/v1/me/**` security, `ApiResponse`, and V3's
`UNIQUE (user_id, lesson_id)` constraint. It adds no separate completion-query endpoint.

**Produces:**
```java
LessonCompletionResponse LearningProgressService.completeLesson(long userId, long lessonId);
CourseProgressResponse LearningProgressService.getCourseProgress(long userId, long courseId);
```

```java
record CourseProgressResponse(long completedLessons, long totalLessons,
    int progressPercent, List<Long> completedLessonIds, NextLessonResponse nextLesson) {}
record LessonCompletionResponse(long lessonId, boolean completed,
    LocalDateTime completedAt) {}
```

```text
PUT /api/v1/me/lessons/{lessonId}/completion
GET /api/v1/me/courses/{courseId}/progress
```

- [ ] **Step 1: Write focused failing tests.** Cover missing authentication, principal
  user ID propagation, unpublished 404, first and repeated PUT both returning HTTP 200
  with the same first timestamp, mapper upsert, two-user isolation, 0/partial/100,
  floor rounding, ordered `completedLessonIds`, skip-learning `nextLesson`, and null
  `nextLesson` after all published Lessons are complete. Add allowed/rejected PUT
  preflight expectations.
- [ ] **Step 2: Prove the red state.** Run:

  ```powershell
  Set-Location stockmentor-backend
  mvn.cmd -Dtest=MyBatisLearningProgressRepositoryTest,LearningProgressServiceTest,LearningProgressControllerTest,SecurityConfigTest test
  ```

  Expect failure from missing progress types/endpoints and PUT CORS support; record
  the real exit code and representative assertion/compile error.
- [ ] **Step 3: Implement concurrency-safe persistence.** The Mapper uses parameterized
  `INSERT ... ON DUPLICATE KEY UPDATE id = id`; the first insert uses
  `CURRENT_TIMESTAMP`, and the duplicate branch cannot change `completed_at` or
  `created_at` or escape to `GlobalExceptionHandler`. Select the row by both
  `user_id` and `lesson_id` after upsert.
- [ ] **Step 4: Implement database-side progress queries.** COUNT total published
  Lessons; COUNT completed published Lessons with current `user_id`; select ordered
  completed IDs with current `user_id`, target `course_id`, and publication filters;
  select the first published incomplete Lesson using `NOT EXISTS`, stable ordering,
  and `LIMIT 1`. Do not load all rows into Java to count/filter.
- [ ] **Step 5: Implement Service and Controller.** Validate publication through Task 1,
  take `userId` only from `@AuthenticationPrincipal`, calculate zero as 0 and otherwise
  use `BigDecimal` with `RoundingMode.DOWN`; return 100 only when every published
  Lesson is complete. Add `PUT` to the existing explicit CORS `allowedMethods` list.
- [ ] **Step 6: Run focused tests to green.** Rerun Step 2 and record actual counts and
  exit code; never treat affected rows 0 from the no-op upsert as failure.
- [ ] **Step 7: Verify real-MySQL isolation and idempotency.** Use a disposable migrated
  schema and real HTTP to register two users. Complete different Lessons, compare both
  progress responses, repeat and concurrently issue the same PUT, then query the table
  to prove one `(user_id, lesson_id)` row and unchanged first `completed_at`. Verify
  private requests without/with invalid Token return 401 and PUT preflight respects the
  allowed-origin whitelist. Do not print complete Tokens. Clean all temporary state.
- [ ] **Step 8: Commit only Task 2.** Inspect staged paths and commit with
  `feat: add V0.3 learning progress`.

---

### Task 3: Frontend Course Experience

**Deliverable:** Anonymous users can browse courses and read safe Lesson Markdown;
authenticated users see persisted completion state and can complete a Lesson without
client-side progress fabrication.

**Files:**

- Modify: `stockmentor-frontend/package.json`, `stockmentor-frontend/package-lock.json`
- Create: `stockmentor-frontend/src/features/course/types/course.ts`
- Create: `stockmentor-frontend/src/features/course/api/courseApi.ts`, `progressApi.ts`
- Create: `stockmentor-frontend/src/features/course/components/MarkdownContent.vue`,
  `MarkdownContent.spec.ts`
- Create: `stockmentor-frontend/src/features/course/views/CoursesView.vue`,
  `CourseDetailView.vue`, `LessonView.vue`
- Test: `stockmentor-frontend/src/features/course/views/CoursesView.spec.ts`,
  `CourseDetailView.spec.ts`, `LessonView.spec.ts`
- Modify: `stockmentor-frontend/src/router/index.ts`
- Modify test: `stockmentor-frontend/src/router/routerGuards.spec.ts`

**Consumes:** Existing Axios instance with `/api/v1` base URL and Token/401 handlers;
existing `AuthStore.isAuthenticated`; Task 1 public APIs and Task 2 private APIs.

**Produces:**
```ts
listCourses(): Promise<AxiosResponse<ApiResponse<CourseSummary[]>>>
getCourse(courseId: number): Promise<AxiosResponse<ApiResponse<CourseDetail>>>
getLesson(lessonId: number): Promise<AxiosResponse<ApiResponse<LessonDetail>>>
completeLesson(lessonId: number): Promise<AxiosResponse<ApiResponse<LessonCompletion>>>
getCourseProgress(courseId: number): Promise<AxiosResponse<ApiResponse<CourseProgress>>>
```

`CourseProgress` exactly includes `completedLessons`, `totalLessons`,
`progressPercent`, `completedLessonIds`, and nullable `nextLesson`. Public routes are
`/courses`, `/courses/:courseId`, and `/lessons/:lessonId` with no `requiresAuth`.

- [ ] **Step 1: Write focused failing component/API/route tests.** Cover loading,
  empty, safe public error, ordered rendering, anonymous/login differences, public
  route metadata and failed session restoration, course completion marks, Lesson refresh from
  `completedLessonIds`, pending-button suppression, completion followed by progress
  reload, `nextLesson`, and no 401 loop. Test that raw HTML, event attributes, and
  dangerous-protocol links do not create executable DOM.
- [ ] **Step 2: Prove the red state.** From `stockmentor-frontend`, run:

  ```powershell
  npm.cmd run test:unit -- src/features/course src/router/routerGuards.spec.ts
  ```

  Expect missing modules/routes and assertions to fail; record exit code and a concise
  failure sample.
- [ ] **Step 3: Add only required dependencies and contracts.** Install `markdown-it`
  and its TypeScript declarations, update the lockfile through npm, define exact API
  response types, and keep calls in the two feature API modules using relative paths
  `/courses`, `/lessons/...`, and `/me/...`.
- [ ] **Step 4: Implement safe rendering and public pages.** Centralize one
  `MarkdownIt({ html: false })` instance in `MarkdownContent.vue`; do not accept user
  Markdown. Implement explicit loading/empty/error/success states and keep anonymous
  Course/Lesson requests independent of private progress.
- [ ] **Step 5: Implement authenticated completion state.** Course detail requests
  progress only when authenticated. Lesson view loads Lesson first, then progress by
  returned `courseId`; derive completed state from `completedLessonIds`. After PUT,
  reload progress and update completed/next-Lesson UI; never increment locally.
- [ ] **Step 6: Run focused tests to green.** Rerun Step 2, then run
  `npm.cmd run type-check`; record actual exit codes. This is focused frontend
  verification, not the final production build.
- [ ] **Step 7: Commit only Task 3.** Inspect the dependency diff for only markdown-it,
  inspect staged paths, and commit with `feat: add V0.3 course learning views`.

---

### Task 4: Dashboard + Integration

**Deliverable:** The V0.2 placeholder becomes the V0.3 learning Dashboard and the
verified navigation loop is: anonymous Courses → Lesson → login → completion →
Dashboard progress → next Lesson.

**Files:**

- Modify: `stockmentor-frontend/src/features/auth/views/AuthDashboardView.vue`
- Modify test: `stockmentor-frontend/src/features/auth/views/AuthDashboardView.spec.ts`
- Modify as needed for navigation only: `stockmentor-frontend/src/features/course/views/CoursesView.vue`,
  `CourseDetailView.vue`, `LessonView.vue` and their existing Task 3 specs
- Modify: `README.md`
- Create: `docs/changelog/2026-08-10-v0.3-course-progress.md`

**Consumes:** Task 3 `listCourses`, `getCourseProgress`, course routes/types, and
Lesson navigation; existing AuthStore profile/logout behavior.

**Produces:** An authenticated Dashboard that selects the first ordered Course without
hard-coding a seed ID, avoids progress calls for an empty list, displays
`completedLessons / totalLessons`, `progressPercent`, `nextLesson`, continue-learning,
and a 100% complete state. It preserves Profile and local logout and adds no V0.6 data.

- [ ] **Step 1: Write the focused failing Dashboard/navigation tests.** Cover loading,
  empty and safe errors; first-course selection; progress and `completedLessonIds`
  reload after remount; next-Lesson link; 100% state; no progress request for no
  courses; Profile/logout preservation; and navigation links among Dashboard,
  Courses, CourseDetail, and Lesson.
- [ ] **Step 2: Prove the red state.** Run:

  ```powershell
  Set-Location stockmentor-frontend
  npm.cmd run test:unit -- src/features/auth/views/AuthDashboardView.spec.ts src/features/course
  ```

  Expect the V0.2 placeholder assertions/new V0.3 expectations to fail; record the
  actual exit code and representative mismatch.
- [ ] **Step 3: Implement the minimal Dashboard and navigation closure.** Fetch Courses,
  select the first ordered item, fetch its private progress, and render only approved
  V0.3 learning data. Use `nextLesson.id` for continue-learning and show completion
  when progress is 100/null next Lesson. Keep local logout and Profile navigation.
- [ ] **Step 4: Run focused Task 4 tests.** Rerun Step 2 and the router tests. Fix only
  current behavior until those tests pass; record counts and exit codes.
- [ ] **Step 5: Run the single final Review.** Review the combined four-Task diff once,
  limited to JWT/public-vs-private boundaries, current-user SQL isolation, first-time
  completion idempotency, unpublished leakage, SQL/N+1, clear correctness errors, and
  divergence from the approved Design. Record findings; fix only validated findings
  and rerun their focused tests. Do not launch a rereviewer.
- [ ] **Step 6: Run final backend verification once.** Use Java 17 and execute:

  ```powershell
  Set-Location stockmentor-backend
  mvn.cmd clean test
  mvn.cmd clean package
  ```

  Record each exit code, Maven build result, and actual test count; do not infer one
  command from the other.
- [ ] **Step 7: Run final frontend verification once.** Execute from
  `stockmentor-frontend`:

  ```powershell
  Set-Location stockmentor-frontend
  npm.cmd ci
  npm.cmd run test:unit
  npm.cmd run type-check
  npm.cmd run build
  ```

  Record each exit code, test count, type-check result, and production-build result.
- [ ] **Step 8: Verify a fresh MySQL/HTTP runtime.** Create a disposable isolated MySQL
  8 schema and migrate V1 → V2 → V3. Verify Flyway history; the four V0.3 tables;
  seed counts/order; indexes, foreign keys, and unique key; published filters; two-user
  isolation; repeat/concurrent upsert with unchanged first time; 0/partial/100;
  `completedLessonIds`; `nextLesson`; health and OpenAPI. Verify public GET with no,
  invalid, and expired Token remains public while `/api/v1/me/**` returns 401.
- [ ] **Step 9: Verify the real browser smoke flow.** In a clean browser session visit
  `/courses`, open a Lesson anonymously, log in, navigate back to it, complete it, and refresh
  completion, open Dashboard, confirm changed percentage, and continue to next Lesson.
  Complete all Lessons or use isolated fixture progress to verify the 100% state.
- [ ] **Step 10: Security, documentation, and cleanup.** Scan tracked changes for real
  credentials/secrets and logs for password/full-Token leakage. Update README and the
  changelog with an `Asia/Shanghai` completion time in `YYYY-MM-DD HH:mm:ss` format and
  only commands actually run, exit codes, test counts, runtime evidence, errors/root
  causes, warnings, and unfinished items. Stop backend/frontend/MySQL test processes
  and prove no temporary ports, schemas, data directories, or control files remain.
- [ ] **Step 11: Commit only Task 4.** Confirm the combined implementation is clean,
  stage the Dashboard/integration/docs changes, and commit with
  `feat: complete V0.3 course progress`.

## Completion gate

V0.3 is complete only when all four Task commits exist, the single final Review has no
unresolved validated findings, every final command and runtime check has recorded
evidence, the worktree is clean, and no V0.4 work has started. Do not merge or push
unless the user gives a separate instruction.
