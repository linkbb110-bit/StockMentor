# StockMentor V0.4 题库与错题设计规范

- 设计状态：产品与技术决策已确认，业务尚未实施
- 决策日期：2026-08-14
- 目标分支：`codex/v0.4-quiz-wrong-questions`
- 基线：V0.3 已通过 PR #3 合并到 `main`，基线提交为 `55e20a33ca50ae31091117f60d170dcb25408ad3`
- 文档性质：V0.4 Implementation Plan 的设计输入

---

## 1. 阶段目标

V0.4 实现 **Objective Quiz + Wrong Question Review** 学习闭环：

```text
Lesson → 课后测验 → 客观题提交 → 后端评分 → 正确性与解析
       → 答错进入错题本 → 重新作答 → 答对后 MASTERED
```

### 1.1 包含范围

- `SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE` 三种客观题。
- 每个已发布 Lesson 对应一个已发布 Quiz。
- Flyway V4 创建题库、答题记录和错题状态表。
- 为现有二十个 Lesson 初始化二十个 Quiz、四十道原创题及选项。
- 匿名读取课后测验，不泄露正确答案或解析。
- 登录用户提交完整 Quiz，由后端校验、评分并创建不可变 Attempt。
- 答错自动进入错题本；错题重做答对后转为 `MASTERED`。
- Vue 提供 Quiz、结果和错题复习体验。
- 聚焦测试以及真实 MySQL、HTTP、浏览器最终验证。

### 1.2 明确不做

- `SHORT_ANSWER` 提交、简答评分、AI Tutor 或 Mock AI。
- Attempt History、考试系统、证书、排行榜、搜索或题库 CMS/Admin。
- 用户手动标记掌握、删除错题或修改历史 Attempt。
- Dashboard 正确率、待复习数、薄弱知识点、最近测验或综合建议聚合。
- 公司分析、虚拟投资、V0.5+、Redis、MQ 或 Idempotency-Key。
- Quiz 完成自动修改 Lesson completion。

V0.4 继续保持投资教育边界：题目不得包含具体证券建议、短期预测或收益承诺。

## 2. 现有基线与模块边界

V0.4 复用现有 `ApiResponse<T>`、`BusinessException`、`ErrorCode`、
`GlobalExceptionHandler`、`AuthenticatedUser`、JWT 过滤链、MyBatis-Plus、
Flyway、AuthStore、sessionStorage、Axios 认证拦截器和 Vue Router 守卫。

后端新增独立包 `com.stockmentor.quiz`，保持：

```text
Controller → Service → Repository → MyBatis Mapper → MySQL
```

- Controller 只做协议转换、Bean Validation、认证主体读取和 Service 调用。
- Service 负责公开可见性、完整提交校验、事务、评分编排和错题状态转换。
- Repository/Mapper 负责参数化查询和原子持久化，不承载页面或状态机编排。
- Entity、请求 DTO、内部投影和响应 VO 分离；Controller 不返回 Entity。
- Quiz 前端放在 `src/features/quiz`，不塞入 `features/course`。
- 认证仍只有现有 AuthStore/sessionStorage 一套状态。

## 3. 领域规则

### 3.1 Quiz 与 Lesson

- 一个 Lesson 最多关联一个 Quiz，由 `UNIQUE (lesson_id)` 保证。
- 公开 Quiz 必须同时满足 Course、Lesson、Quiz 已发布。
- 公开返回和提交只包含该 Quiz 下已发布 Question 与已发布 Option。
- 已发布 Quiz 没有任何已发布 Question 时，按不可用资源返回 404。
- V0.4 每道 Question 只属于一个 Quiz；`quiz_question.question_id` 唯一，
  因而错题列表中的 `lessonId` 与 `lessonTitle` 没有歧义。
- Quiz Attempt 与 Lesson completion 是两个独立状态，互不自动更新。

### 3.2 题型与选择基数

`SINGLE_CHOICE` 与 `TRUE_FALSE`：

- 必须且只能提交一个 Option。
- 选择集合与正确集合完全相等时才正确。

`MULTIPLE_CHOICE`：

- 至少提交一个 Option。
- `selectedOptionIds` 与 `correctOptionIds` 按集合完全相等时才正确。
- 输入顺序不影响结果；少选、多选或混入错误选项均为错误。

所有题型共同规则：

- 同一答案内重复 Option ID 属于非法请求，不静默去重。
- Option 必须属于当前 Question 且处于已发布状态。
- 正确与已选 Option ID 在响应中按 `question_option.sort_order, id` 稳定排序。

### 3.3 完整 Quiz 提交

`POST /api/v1/me/quizzes/{quizId}/attempts` 的 `answers` 必须与当前 Quiz 的
全部已发布 Question 一一对应：不缺题、不多题、不重复 Question ID。
Question 或 Option 越界、未发布、类型选择基数非法时返回
HTTP 400 / `VALIDATION_FAILED`，且不创建任何持久化记录。

一次合法 POST 创建一条新的不可变 `quiz_attempt`。重复提交同一内容仍创建
新的 Attempt；前端只用 pending disabled 防止普通双击，不引入提交幂等语义。

评分百分比为整数：

```text
correctCount == totalQuestions → 100
其他情况 → floor(correctCount * 100 / totalQuestions)
```

可用 Quiz 的 `totalQuestions` 必须大于零。

### 3.4 提交事务

一次提交在单个 `@Transactional` Service 方法中完成：

1. 读取已发布 Course/Lesson/Quiz。
2. 批量读取全部已发布 Question 与 Option。
3. 在任何写入前完成问题集合、选项归属和选择基数校验。
4. 使用共享评分组件计算每题结果和总分。
5. 插入 `quiz_attempt`。
6. 插入每条 `quiz_answer` 及其 `quiz_answer_option`。
7. 根据每题结果更新当前用户的 `wrong_question`。

任一步抛出异常均回滚 Attempt、Answer、AnswerOption 和 WrongQuestion 变更。

### 3.5 Wrong Question 状态机

| 当前记录 | 本次结果 | 新状态 | error_count | last_wrong_at | mastered_at | last_reviewed_at |
|---|---|---|---|---|---|---|
| 不存在 | 正确 | 不创建 | — | — | — | — |
| 不存在 | 错误 | PENDING | 1 | now | null | now |
| PENDING | 错误 | PENDING | +1 | now | null | now |
| PENDING | 正确 | MASTERED | 不变 | 不变 | now | now |
| MASTERED | 错误 | PENDING | +1 | now | null | now |
| MASTERED | 正确 | MASTERED | 不变 | 不变 | 保持原值 | now |

- 不提供手动掌握或删除接口。
- Quiz 提交和错题 Review 共用相同状态规则。
- 错误更新使用 `UNIQUE (user_id, question_id)` 与原子 MySQL upsert，
  并发时不会创建重复行或丢失 `error_count` 增量。
- 正确更新只针对已存在的当前用户记录；从未错过且答对不会插入记录。
- Wrong-question review 不创建 `quiz_attempt`，避免污染后续 Quiz 正确率。

## 4. 数据库设计

V4 文件为：
`stockmentor-backend/src/main/resources/db/migration/V4__create_quiz_wrong_question_tables.sql`。
不得修改 V1、V2、V3。空 MySQL 8.4 数据库必须按 V1 → V2 → V3 → V4 成功迁移。

新表统一使用 InnoDB、utf8mb4、utf8mb4_0900_ai_ci、BIGINT 自增主键、
DATETIME 和 snake_case。V0.4 不使用 `deleted` 或逻辑删除。

### 4.1 question

- `id BIGINT AUTO_INCREMENT`，主键。
- `type VARCHAR(32) NOT NULL`，检查值仅为三种客观题型。
- `stem VARCHAR(1000) NOT NULL`。
- `explanation VARCHAR(2000) NOT NULL`。
- `published TINYINT NOT NULL DEFAULT 0`。
- `created_at`、`updated_at` 使用现有时间规则。
- `idx_question_published (published, id)`。

### 4.2 question_option

- `id BIGINT AUTO_INCREMENT`，主键。
- `question_id BIGINT NOT NULL`，外键到 `question.id`，更新/删除 RESTRICT。
- `option_key VARCHAR(20) NOT NULL`。
- `content VARCHAR(500) NOT NULL`。
- `is_correct TINYINT NOT NULL DEFAULT 0`。
- `published TINYINT NOT NULL DEFAULT 0`。
- `sort_order INT NOT NULL`。
- `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`。
- `UNIQUE (question_id, option_key)`。
- `UNIQUE (question_id, sort_order)`。
- `idx_question_option_published_sort (question_id, published, sort_order, id)`。

`published` 是提交规则“拒绝未发布 Option”的唯一数据来源。TRUE_FALSE 也用两条
Option，键为 `TRUE` 与 `FALSE`；Question 不增加布尔答案或答案字符串字段。

### 4.3 quiz 与 quiz_question

`quiz`：

- `id BIGINT AUTO_INCREMENT`，主键。
- `lesson_id BIGINT NOT NULL`，外键到 `lesson.id`，更新/删除 RESTRICT。
- `title VARCHAR(100) NOT NULL`、`summary VARCHAR(500) NOT NULL`。
- `published TINYINT NOT NULL DEFAULT 0`。
- `created_at`、`updated_at` 使用现有时间规则。
- `UNIQUE (lesson_id)`，另有 `idx_quiz_published (published, id)`。

`quiz_question`：

- `id BIGINT AUTO_INCREMENT`，主键。
- `quiz_id BIGINT NOT NULL`、`question_id BIGINT NOT NULL`，均为 RESTRICT 外键。
- `sort_order INT NOT NULL`。
- `UNIQUE (quiz_id, question_id)`。
- `UNIQUE (quiz_id, sort_order)`。
- `UNIQUE (question_id)`，锁定 V0.4 一题只属于一个 Quiz 的语义。

### 4.4 quiz_attempt

- `id BIGINT AUTO_INCREMENT`，主键。
- `user_id BIGINT NOT NULL`，外键到 `sys_user.id`。
- `quiz_id BIGINT NOT NULL`，外键到 `quiz.id`。
- `total_questions SMALLINT UNSIGNED NOT NULL`，必须大于零。
- `correct_count SMALLINT UNSIGNED NOT NULL`，介于零与总题数之间。
- `score_percent TINYINT UNSIGNED NOT NULL`，范围 0–100。
- `submitted_at DATETIME NOT NULL`、`created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`。
- `idx_quiz_attempt_user_submitted (user_id, submitted_at, id)`。
- `idx_quiz_attempt_quiz (quiz_id, id)`。

不存在 `(user_id, quiz_id)` 唯一约束，允许重复做题。

### 4.5 quiz_answer 与 quiz_answer_option

`quiz_answer`：

- `id BIGINT AUTO_INCREMENT`，主键。
- `attempt_id BIGINT NOT NULL`，外键到 `quiz_attempt.id`。
- `question_id BIGINT NOT NULL`，外键到 `question.id`。
- `is_correct TINYINT NOT NULL`。
- `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`。
- `UNIQUE (attempt_id, question_id)`，另建 `idx_quiz_answer_question (question_id)`。

`quiz_answer_option`：

- `quiz_answer_id BIGINT NOT NULL`，外键到 `quiz_answer.id`。
- `option_id BIGINT NOT NULL`，外键到 `question_option.id`。
- 联合主键 `(quiz_answer_id, option_id)`。
- `idx_quiz_answer_option_option (option_id, quiz_answer_id)` 支持反向外键查询。

多选结果不存为 CSV 或 JSON。

### 4.6 wrong_question

- `id BIGINT AUTO_INCREMENT`，主键。
- `user_id BIGINT NOT NULL`，外键到 `sys_user.id`。
- `question_id BIGINT NOT NULL`，外键到 `question.id`。
- `status VARCHAR(16) NOT NULL`，检查值为 `PENDING` 或 `MASTERED`。
- `error_count INT UNSIGNED NOT NULL`，必须大于零。
- `last_wrong_at DATETIME NOT NULL`。
- `mastered_at DATETIME NULL`。
- `last_reviewed_at DATETIME NOT NULL`。
- `created_at`、`updated_at` 使用现有时间规则。
- `UNIQUE (user_id, question_id)`。
- `idx_wrong_question_user_pending (user_id, status, last_wrong_at, id)`。
- `idx_wrong_question_user_mastered (user_id, status, mastered_at, id)`。
- `idx_wrong_question_question (question_id)`。

所有外键均显式使用 ON DELETE/ON UPDATE RESTRICT。

## 5. V4 初始化内容

V4 使用 Flyway 一次性初始化二十个已发布 Quiz，每个现有 Lesson 一个；每个 Quiz
包含两道已发布原创客观题，共四十题。Question、Option 和 mapping 只由 Flyway seed，
Java 启动时不动态生成内容。

| Lesson | Q1 类型/重点 | Q2 类型/重点 |
|---|---|---|
| 1 | SINGLE/股票与所有权 | TRUE_FALSE/基金与债券权利 |
| 2 | MULTIPLE/指数样本与权重 | SINGLE/指数解读边界 |
| 3 | SINGLE/收益组成 | MULTIPLE/成本与期间口径 |
| 4 | TRUE_FALSE/风险类型 | MULTIPLE/波动与流动性 |
| 5 | SINGLE/价值创造链 | TRUE_FALSE/收入与价值 |
| 6 | MULTIPLE/商业模式要素 | SINGLE/订阅与单次销售 |
| 7 | SINGLE/利润表层次 | TRUE_FALSE/期间记录边界 |
| 8 | MULTIPLE/利润持续性 | SINGLE/一次性项目 |
| 9 | TRUE_FALSE/会计恒等式 | MULTIPLE/资产质量 |
| 10 | SINGLE/偿债期限 | TRUE_FALSE/总负债局限 |
| 11 | SINGLE/现金流分类 | MULTIPLE/现金净额结构 |
| 12 | TRUE_FALSE/权责发生制 | SINGLE/应收与现金差异 |
| 13 | SINGLE/市盈率含义 | MULTIPLE/低市盈率误区 |
| 14 | TRUE_FALSE/市净率边界 | SINGLE/跨行业比较 |
| 15 | MULTIPLE/竞争力量 | TRUE_FALSE/行业规模误区 |
| 16 | SINGLE/优势证据 | MULTIPLE/不可单独证明的信号 |
| 17 | TRUE_FALSE/分散边界 | MULTIPLE/风险来源相关性 |
| 18 | SINGLE/风险承受维度 | TRUE_FALSE/单次经历局限 |
| 19 | MULTIPLE/事实假设风险 | SINGLE/可检验记录 |
| 20 | TRUE_FALSE/结果偏差 | MULTIPLE/过程复盘维度 |

题干、选项和解析必须简洁原创、与 V3 Markdown 一致、没有第三方题库内容。
SINGLE 与 MULTIPLE 使用稳定的 `A/B/C/D` Option key；TRUE_FALSE 使用
`TRUE/FALSE`。种子必须满足每题至少一个正确 Option，且单选/判断恰好一个正确项。

## 6. 后端组件与查询

### 6.1 组件职责

- `QuizController`：公开 Quiz GET。
- `QuizAttemptController`：从 `AuthenticatedUser.userId()` 提交 Attempt。
- `WrongQuestionController`：当前用户错题列表与 Review。
- `QuizQueryService`：公开发布过滤、批量目录组装和无答案泄漏映射。
- `QuizAttemptService`：完整提交校验、事务、评分、Attempt 持久化与错题更新。
- `WrongQuestionService`：按用户读取错题并执行 Review 状态转换。
- `ObjectiveQuestionScorer`：选择基数校验、Option 归属校验与 exact-set 评分。
- `QuizRepository`：Quiz/Question/Option 读取。
- `QuizAttemptRepository`：Attempt/Answer/AnswerOption 插入。
- `WrongQuestionRepository`：用户隔离列表、存在性读取及原子状态更新。

### 6.2 共享评分契约

评分组件输入 Question type、提交的 Option ID 和按排序加载的内部 Option
（包含 `isCorrect`）；输出：

```text
correct
selectedOptionIds（稳定排序）
correctOptionIds（稳定排序）
```

公开 VO 和错题列表 VO 从不接触内部 `isCorrect`。Quiz Attempt 与 Wrong Review
都调用同一个评分组件，不复制三种题型算法。

### 6.3 查询边界

- 公开 Quiz 使用一次 Course/Lesson/Quiz 上下文查询、一次有序 published Question
  查询、一次按 Question ID 批量读取 published Option；Service 组装，不按题循环查 Option。
- 提交按 Quiz ID 使用同样的批量读取结构，但内部投影包含正确性与解析。
- Wrong list 主查询必须同时包含 `user_id`、状态及 Course/Lesson/Quiz/Question
  published 条件，再批量读取 Option；已下架内容保留数据库记录但不对用户泄露。
- PENDING 按 `last_wrong_at DESC, id DESC`，MASTERED 按
  `mastered_at DESC, id DESC`。
- Review 先按 `user_id + question_id` 和完整发布链读取当前用户记录；不存在、
  属于他人或已下架统一返回 404。
- 所有私有 SQL 都显式包含当前 `user_id`；不提供 `/users/{userId}` 形式接口。

## 7. API 契约

所有路径使用 `/api/v1`，JSON 使用 camelCase，时间使用 ISO 8601，响应继续包装
`ApiResponse<T>`。资源不存在或发布链不可用返回 404 / `RESOURCE_NOT_FOUND`；
非法完整提交返回 400 / `VALIDATION_FAILED`；认证失败沿用统一 401。

### 7.1 GET /api/v1/lessons/{lessonId}/quiz

- 匿名公开，成功 HTTP 200。
- 返回 `id`、`lessonId`、`lessonTitle`、`courseId`、`courseTitle`、
  `chapterId`、`chapterTitle`、`title`、`summary`、`questions`。
- Question：`questionId`、`type`、`stem`、`options`。
- Option：`optionId`、`optionKey`、`content`。
- 禁止返回 `isCorrect`、`correctOptionIds`、`explanation`、Entity 或可推导答案字段。

### 7.2 POST /api/v1/me/quizzes/{quizId}/attempts

- 必须登录；请求体不接受 userId。
- Request：`answers: [{ questionId, selectedOptionIds }]`。
- 成功创建新 Attempt，返回 HTTP 201。
- Response：`attemptId`、`totalQuestions`、`correctCount`、`scorePercent`、`results`。
- Result：`questionId`、`correct`、`selectedOptionIds`、`correctOptionIds`、
  `explanation`，顺序与 Quiz Question 顺序一致。

### 7.3 GET /api/v1/me/wrong-questions

- 必须登录；`status` 仅接受 `PENDING`、`MASTERED`，默认 `PENDING`。
- 成功 HTTP 200，不分页；空结果为 `[]`。
- 返回 `questionId`、`lessonId`、`lessonTitle`、`type`、`stem`、`options`、
  `status`、`errorCount`、`lastWrongAt`、`masteredAt`。
- 列表不返回正确答案、`isCorrect` 或解析。

### 7.4 POST /api/v1/me/wrong-questions/{questionId}/answer

- 必须登录；请求体只有 `selectedOptionIds`，不接受 userId 或状态。
- 仅允许 Review 当前用户已有且发布链仍可见的错题，其他情况 404。
- 成功 HTTP 200。
- 返回 `questionId`、`correct`、`status`、`errorCount`、
  `correctOptionIds`、`explanation`。
- 不创建 Attempt；返回的新状态是前端唯一可信来源。

## 8. Security 与隐私

现有 `PublicCourseGetRequestMatcher` 最小扩展一个精确形状：

```text
GET /api/v1/lessons/{lessonId}/quiz
```

该 matcher 同时用于 Spring Security `permitAll` 和
`JwtAuthenticationFilter.shouldNotFilter`，因此公开 Quiz 即使携带 stale/invalid
Bearer Token 仍按匿名读取。不得泛化为 `/api/v1/lessons/**` 或 `/api/v1/**`。

`POST /api/v1/me/quizzes/**` 与 `/api/v1/me/wrong-questions/**` 不进入 matcher，
始终严格认证。userId 只来自 `AuthenticatedUser.userId()`，不从客户端读取。
User A 即使知道 Question/Attempt ID，也不能查看或改变 User B 数据。

CORS 继续使用现有显式 Origin、Method 和 Header 白名单；现有 allowedMethods 已含
GET/POST/PUT/PATCH/OPTIONS，V0.4 不增加通配 Origin、credentials 或新 Method。

日志不得记录完整 JWT、密码、数据库密码、正确答案集合或完整提交负载。

## 9. 前端设计

### 9.1 结构与路由

新增 `src/features/quiz`，包含 `api`、`types`、`views` 和必要组件。

- `/lessons/:lessonId/quiz`：公开 `QuizView`，不设置 `requiresAuth`。
- `/wrong-questions`：`WrongQuestionsView`，设置 `requiresAuth: true`。
- 现有 course、dashboard、profile 路由权限不变。
- `LessonView` 只增加“课后测验”入口，不改变完成规则。

API 使用现有 Axios `/api/v1` baseURL，仅传相对路径，不创建第二套拦截器或 Token 状态。

### 9.2 QuizView

- 显式处理 loading、error、question、submit pending 和 result。
- SINGLE/TRUE_FALSE 使用 radio，MULTIPLE 使用 checkbox。
- 匿名用户只调用公开 Quiz GET；点击提交时提示登录并导航 `/login`，不调用 `/me/**`。
- 登录用户提交全部选择；pending 时禁用按钮并阻止普通重复点击。
- API 成功前不显示正确性，不在前端保存或计算正确答案与分数。
- 成功后完全消费后端的 count、score、逐题正确性、正确选项和解析。
- 有错题时提供 `/wrong-questions` 入口。
- 本阶段不持久化最后一次结果；刷新后重新显示未提交 Quiz 属于预期行为。

### 9.3 WrongQuestionsView

- 提供 PENDING（默认）与 MASTERED 两个轻量视图。
- 每次切换状态或刷新页面都从 API 重新加载，不使用长期 Pinia 缓存。
- 显示 Lesson、题型、题干、选项、错误次数和相关时间。
- Review pending 时只禁用当前题；失败后恢复且不伪造状态。
- Review 成功后展示后端返回的正确性/解析，并重新加载当前筛选列表；
  PENDING 答对或 MASTERED 答错导致离开当前列表时，以重新加载结果为准。
- 刷新后 PENDING、MASTERED 和 errorCount 必须由后端恢复。

## 10. 测试与真实验证

### 10.1 后端聚焦测试

- Public read：匿名/invalid bearer 200、严格字段无答案/解析泄漏、发布链 404、无 N+1。
- Scorer：单选/判断正确与错误、多个选择非法；多选 exact set、逆序正确、
  少选/多选错误；Option 越界和重复 ID 非法。
- Submission：缺题、多题、重复 Question、跨题 Option、未发布资源、选择基数。
- Transaction：成功写入 Attempt/Answer/AnswerOption/WrongQuestion；模拟持久化失败
  触发回滚，真实 MySQL 再验证零部分记录。
- State machine：六条表格路径全部覆盖。
- Isolation：私有接口匿名 401、A/B 列表与 Review 隔离、下架内容不泄露。

### 10.2 前端聚焦测试

- QuizView：loading、三题型、匿名无私有请求、登录提交、pending、成功结果、
  error recovery、后端分数/解析、错题入口。
- WrongQuestionsView：loading、两个筛选、答错/答对转换、API failure、刷新恢复。
- Router：Quiz public、wrong-questions protected、现有公开/受保护路由回归。

### 10.3 Task 4 最终验证

最终阶段只运行一次完整：

```text
mvn clean test
mvn clean package
npm run test:unit
npm run type-check
npm run build
```

使用隔离 MySQL 8.4 空库验证 V1→V4、八张 V0.4 表、约束/索引及
1 Course / 10 Chapter / 20 Lesson / 20 Quiz / 40 Question。

真实 HTTP 验证公开 Quiz、invalid Token、登录、提交、重复提交产生不同 Attempt、
错题状态、A/B 隔离、下架 404；通过临时数据库故障点验证事务失败后无部分记录。

浏览器 smoke：Lesson → Quiz → 故意答错 → Result → Wrong Questions →
重做答对 → MASTERED → 刷新仍为 MASTERED。结束后清理临时数据库、进程、端口和日志。

## 11. 轻量实施流程

Implementation Plan 固定四个主 Task：

1. Database + Public Quiz Read Model。
2. Scoring + Quiz Attempts + Wrong Question State Machine。
3. Frontend Quiz + Wrong Question Experience。
4. Integration + Docs + Final Verification。

Task 1–3 仅运行聚焦测试与必要 regression；Task 4 才运行完整 suite。
四个 Task 完成后只做一次 whole-branch Review；仅修复真实 Critical/Important，
需要时做一次 scoped re-review，再 fresh verification、push 和 PR。

## 12. 验收标准

1. V4 从空库顺序执行并创建八表、二十 Quiz、四十原创 Question 及完整 mapping/options。
2. 匿名和 invalid/stale Token 可读取精确公开 Quiz GET，且答案与解析不泄漏。
3. 所有私有接口严格认证，userId 仅来自 AuthenticatedUser，A/B 数据隔离。
4. 三种题型选择基数和 exact-set 评分正确，前端不预计算分数。
5. 完整提交校验在写入前完成；合法 POST 创建新 Attempt，任一步失败全部回滚。
6. Attempt、Answer 和选择项规范持久化，不使用 CSV/JSON 答案字段。
7. Wrong Question 六种转换完整，重复错误原子递增，MASTERED 再错恢复 PENDING。
8. 从未错过且答对不创建记录，错题 Review 不创建 Attempt。
9. 下架 Course/Lesson/Quiz/Question 不通过公开或错题列表泄露。
10. QuizView 支持匿名浏览、三题型、pending、结果与错误恢复。
11. WrongQuestionsView 从后端恢复状态，并以后端 Review 响应与重载结果为准。
12. Lesson 入口、公开 Quiz 路由和受保护错题路由不破坏现有权限守卫。
13. Dashboard 不增加 V0.6 聚合，V0.5+ 未提前实现。
14. 完整测试、构建、MySQL、HTTP、浏览器和清理都有实际证据。
15. Git 不含真实密钥、环境文件、测试日志或范围外业务。
