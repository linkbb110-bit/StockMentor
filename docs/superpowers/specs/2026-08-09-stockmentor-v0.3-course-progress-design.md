# StockMentor V0.3 课程与学习进度设计规范

- 设计状态：设计已批准，文档已落地，业务尚未实施
- 决策确认日期：2026-08-09
- 目标分支：codex/v0.3-course-progress
- 前置版本：V0.2 用户与认证已通过 PR #2 合并到 main
- 文档性质：后续 Implementation Plan 的设计输入，不是实施计划

---

## 1. 阶段目标

V0.3 建立第一个可运行的学习闭环：

1. 匿名用户浏览已发布课程、目录和课时正文。
2. 登录用户把课时标记为完成。
3. 系统按用户隔离完成记录并计算课程进度。
4. 系统推荐第一节已发布且当前用户未完成的课时。
5. 前端把 V0.2 占位首页升级为课程进度 Dashboard。
6. Flyway 初始化一门原创入门课程。

本阶段继续保持投资教育边界，不提供具体证券买卖建议、短期预测或收益承诺。

### 1.1 包含范围

- 一门《股票投资基础》课程。
- 十个 Chapter，每章两个 Lesson，共二十个 Lesson。
- 公开课程列表、课程详情和课时详情。
- 登录用户的课时完成写入。
- 完成数、总数、整数进度百分比和下一课。
- CoursesView、CourseDetailView、LessonView 和课程进度 Dashboard。
- 原创、简洁的 Markdown 种子内容。
- 数据库、后端、前端和真实 MySQL/HTTP 流程验证。

### 1.2 明确不做

- CMS、管理员课程后台、课程编辑 API 或运行时内容增删改。
- ADMIN 读取未发布内容的额外权限。
- 取消完成、重置进度或学习历史编辑。
- learning_record、事件表、消息队列或审计事件系统。
- Redis 锁、分布式锁或缓存强制依赖。
- 富文本编辑器、文件上传、封面上传或媒体内容。
- 题库、错题、AI 导师、公司分析和虚拟投资日志。
- V0.6 跨题库、错题、AI 和组合的聚合 Dashboard。

## 2. 现有基线与阶段性差异

V0.3 复用 ApiResponse<T>、BusinessException、ErrorCode、
GlobalExceptionHandler、AuthenticatedUser、JWT 过滤链、
Controller → Service → Repository → Mapper 分层、MyBatis-Plus、
Flyway、AuthStore、sessionStorage、Axios 认证拦截器和 Vue Router 守卫。
所有 V0.3 后端接口统一遵循现有 /api/v1 前缀。

经确认的设计与通用文档存在以下差异：

1. 通用 API 规范要求创建资源返回 201；completion 是幂等状态写入，
   第一次和重复调用均按已批准设计返回 200。
2. 通用 ER 文档使用 learning_progress；V0.3 使用 user_lesson_progress，
   不同时创建两张表。
3. 通用 ER 文档建议主要业务表包含 updated_at 和 deleted；
   V0.3 不增加 deleted，user_lesson_progress 也不增加 updated_at。
4. 通用 ER 与索引文档列出 learning_record；V0.3 明确不创建它。
5. 页面概览提到上一节和下一节；V0.3 只保证基于个人进度的下一课。
6. 完整学习 Dashboard 属于 V0.6；V0.3 Dashboard 只展示课程进度。

以上均为已确定的阶段性规则，不授权实施阶段扩展范围。

## 3. 领域模型与业务规则

内容层级为 Course → Chapter → Lesson：

- Course 是公开课程容器，拥有 published。
- Chapter 负责课程内分组和排序，不设置 published。
- Lesson 保存 Markdown 正文、预计学习时长、排序和 published。
- user_lesson_progress 表示某用户已经完成某课时。

### 3.1 发布与可见性

- 课程列表只返回 published = true 的 Course。
- 课程详情要求 Course 已发布，并返回全部有序 Chapter。
- Chapter 下只返回 published = true 的 Lesson 摘要。
- 课时详情要求 Lesson 和所属 Course 都已发布。
- 未发布与不存在统一返回 HTTP 404 / RESOURCE_NOT_FOUND。
- ADMIN 和普通用户使用相同读取规则。
- 私有进度接口不能成为读取未发布内容的旁路。

### 3.2 完成规则

- 只支持标记完成，不支持取消。
- (user_id, lesson_id) 唯一。
- 第一次和重复完成均返回 HTTP 200。
- 第一次完成创建记录并写入 completed_at。
- 重复 PUT 是成功的 no-op，不新增记录，也不改写首次 completed_at。
- UNIQUE (user_id, lesson_id) 与 MySQL upsert 共同保证并发幂等。
- 只能完成已发布 Course 下的已发布 Lesson。
- userId 只来自 AuthenticatedUser；请求体、路径和查询参数均不接受 userId。

### 3.3 Progress 规则

- totalLessons 只统计目标 Course 下已发布 Lesson。
- completedLessons 只统计当前用户已完成且仍已发布的 Lesson。
- completedLessonIds 返回上述已完成且仍已发布 Lesson 的 ID，
  按 Chapter.sort_order、Lesson.sort_order 排序。
- progressPercent 是 0 到 100 的整数；totalLessons = 0 时固定为 0。
- totalLessons > 0 时，按 completedLessons × 100 ÷ totalLessons 向下取整。
- 只有 completedLessons = totalLessons 且 totalLessons > 0 时才能达到 100。
- nextLesson 是按 Chapter.sort_order、Lesson.sort_order 排序后的
  第一节已发布且当前用户未完成的 Lesson。
- 用户可以跳着学习；全部完成时 nextLesson = null。
- 已完成 Lesson 后来未发布时，不计入分子或分母。

## 4. 数据库设计

后续 V3 迁移创建四张表并写入初始化课程。
不得修改已应用的 V1 或 V2。
空 MySQL 8 数据库必须可按 V1 → V2 → V3 顺序执行。

四表统一使用 InnoDB、utf8mb4、utf8mb4_0900_ai_ci、
BIGINT 自增主键、DATETIME 和 snake_case。
V3 不增加逻辑删除、版本号、事件字段或额外业务表。

### 4.1 course

- id BIGINT NOT NULL AUTO_INCREMENT，主键。
- title VARCHAR(100) NOT NULL。
- summary VARCHAR(500) NOT NULL。
- cover_url VARCHAR(500) NULL；为空时前端使用占位样式。
- published TINYINT NOT NULL DEFAULT 0。
- sort_order INT NOT NULL。
- created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP。
- updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP。
- idx_course_published_sort (published, sort_order, id)。
- sort_order 相同时按 id 升序保持稳定结果。

### 4.2 chapter

- id BIGINT NOT NULL AUTO_INCREMENT，主键。
- course_id BIGINT NOT NULL。
- title VARCHAR(100) NOT NULL。
- summary VARCHAR(500) NOT NULL。
- sort_order INT NOT NULL。
- created_at 与 updated_at 沿用 course 规则。
- uk_chapter_course_sort (course_id, sort_order)。
- fk_chapter_course：course_id → course.id。
- 外键显式使用 ON DELETE RESTRICT 和 ON UPDATE RESTRICT。

### 4.3 lesson

- id BIGINT NOT NULL AUTO_INCREMENT，主键。
- chapter_id BIGINT NOT NULL。
- title VARCHAR(100) NOT NULL。
- summary VARCHAR(500) NOT NULL。
- content_md TEXT NOT NULL。
- estimated_minutes SMALLINT UNSIGNED NOT NULL，必须大于 0。
- sort_order INT NOT NULL。
- published TINYINT NOT NULL DEFAULT 0。
- created_at 与 updated_at 沿用 course 规则。
- uk_lesson_chapter_sort (chapter_id, sort_order)。
- idx_lesson_chapter_published_sort (chapter_id, published, sort_order, id)。
- fk_lesson_chapter：chapter_id → chapter.id，删除和更新使用 RESTRICT。

### 4.4 user_lesson_progress

- id BIGINT NOT NULL AUTO_INCREMENT，主键。
- user_id BIGINT NOT NULL。
- lesson_id BIGINT NOT NULL。
- completed_at DATETIME NOT NULL，表示首次完成时间。
- created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP。
- uk_user_lesson_progress_user_lesson (user_id, lesson_id)。
- idx_user_lesson_progress_lesson (lesson_id)。
- fk_user_lesson_progress_user：user_id → sys_user.id。
- fk_user_lesson_progress_lesson：lesson_id → lesson.id。
- 两个外键的删除和更新均使用 RESTRICT。
- 不添加 updated_at 或 deleted。

现有 V1/V2 没有外键命名先例；上述 fk_ 前缀是 V0.3 的明确约定。
索引继续沿用现有 uk_ 与 idx_ 风格，并在实现后用 EXPLAIN 验证。

### 4.5 并发幂等

完成写入使用 MySQL INSERT ... ON DUPLICATE KEY UPDATE。
UNIQUE (user_id, lesson_id) 是并发正确性的最终保护，
不依赖“先查后插”避免竞争。

第一次插入写入 completed_at；重复键分支执行成功的 no-op，
保留 id、第一次 completed_at 和 created_at。
Service 不根据 affected rows 为 0 判定失败。
upsert 必须消化重复键，不能让现有全局 DuplicateKeyException
处理逻辑把它误映射为 USER_EMAIL_ALREADY_EXISTS。

完成事务：

1. 按公开可见条件确认 Lesson。
2. 使用 AuthenticatedUser.userId() upsert。
3. 按 user_id 与 lesson_id 查询完成记录。
4. 返回稳定的完成结果。

## 5. 初始化课程内容

V3 初始化一条已发布 Course、十条 Chapter 和二十条已发布 Lesson。
Course.sort_order = 1，cover_url = null；Chapter 顺序为 1–10，
每章 Lesson 顺序为 1、2，estimated_minutes 均为正数。

每节正文统一使用四个轻量二级段落：

1. 概念
2. 简单例子
3. 常见误区
4. 学习小结

正文必须原创，不复制教材，不推荐具体证券，不承诺收益。

| Chapter | Lesson 1 | Lesson 2 |
|---|---|---|
| 1. 股票、基金、债券和指数 | 股票、基金与债券的基本权利 | 指数如何描述一组资产 |
| 2. 收益与风险 | 投资收益从哪里来 | 风险不只是价格波动 |
| 3. 公司和商业模式 | 公司如何创造价值 | 用商业模式框架理解公司 |
| 4. 利润表 | 利润表的收入、成本与利润 | 识别利润质量和一次性项目 |
| 5. 资产负债表 | 资产、负债与所有者权益 | 从资产负债表观察偿债风险 |
| 6. 现金流量表 | 经营、投资与筹资现金流 | 利润与现金为什么不同 |
| 7. 常见估值指标 | 市盈率表达了什么 | 市净率等指标的适用边界 |
| 8. 行业与竞争分析 | 行业结构与竞争强度 | 竞争优势需要哪些证据 |
| 9. 风险和分散 | 单一风险与分散原则 | 风险承受能力与学习边界 |
| 10. 投资复盘 | 记录假设、证据与风险 | 复盘过程而不只看结果 |

Flyway 种子是 V0.3 唯一内容生产方式，不提供运行时编辑入口。

## 6. 后端与查询设计

V0.3 归入 com.stockmentor.course 模块：

- CourseController：公开课程列表和详情。
- LessonController：公开课时详情。
- LearningProgressController：当前用户完成与进度。
- CourseQueryService：发布过滤、目录组装和课时详情。
- LearningProgressService：完成幂等、计数、百分比和下一课。
- CourseRepository：Course、Chapter、Lesson 公开读取。
- LearningProgressRepository：upsert、完成计数、有序已完成课时 ID 和下一课查询。
- Mapper：参数化 SQL，不承载业务判断。
- Entity 与对外 VO 分离，Entity 不从 Controller 返回。

Controller 只做协议转换、认证主体读取和 Service 调用。
私有 Controller 直接从 @AuthenticationPrincipal AuthenticatedUser 取 userId。

### 6.1 课程目录

课程详情固定使用三次批量查询：

1. 一条已发布 Course。
2. 该 Course 的全部有序 Chapter。
3. 这些 Chapter 下全部已发布 Lesson。

Service 按 course_id 和 chapter_id 组装树，不按 Chapter 循环查 Lesson。

### 6.2 课时与进度

- 课时详情 SQL 同时限制 Lesson.published = 1 和 Course.published = 1。
- Progress 先确认 Course 已发布，避免把 404 与零课时混淆。
- totalLessons 与 completedLessons 使用数据库 COUNT。
- 完成 COUNT 的 JOIN 条件必须包含当前 user_id。
- completedLessonIds 按当前 user_id、目标 course_id 和发布条件查询，
  按 Chapter.sort_order、Lesson.sort_order、Chapter.id、Lesson.id 排序。
- nextLesson 使用 NOT EXISTS 排除该用户已完成记录，
  按 Chapter.sort_order、Lesson.sort_order、Chapter.id、Lesson.id 排序并 LIMIT 1。
- 不把全部 Lesson 和进度加载到 Java 后 count/filter。
- 所有读写 user_lesson_progress 的 SQL 都包含当前 user_id。
- 公开读取使用只读事务；completion 使用单个短事务。

## 7. API 设计

所有接口继续返回 ApiResponse<T>；JSON 使用 camelCase，时间使用 ISO 8601。
未发布与不存在返回 404 / RESOURCE_NOT_FOUND。
私有接口沿用现有统一 401。
本阶段优先复用现有错误码，不批量新增课程专用错误码。

### 7.1 GET /api/v1/courses

- 匿名公开，成功返回 200。
- 只返回已发布 Course，按 sort_order、id 升序。
- V0.3 不分页；空结果 data = []。
- CourseSummary：id、title、summary、coverUrl。

### 7.2 GET /api/v1/courses/{courseId}

- 匿名公开，成功返回 200。
- CourseDetail：id、title、summary、coverUrl、chapters。
- ChapterSummary：id、title、summary、lessons。
- LessonSummary：id、title、summary、estimatedMinutes。
- Chapter 与 Lesson 都按 sort_order、id 升序。
- 只返回已发布 Lesson；没有已发布 Lesson 的 Chapter 保留空列表。

### 7.3 GET /api/v1/lessons/{lessonId}

- 匿名公开，成功返回 200。
- LessonDetail：id、title、summary、contentMd、estimatedMinutes。
- 同时返回 course 的 id、title 和 chapter 的 id、title。
- 不返回数据库 Entity、published 字段或用户进度。

### 7.4 PUT /api/v1/me/lessons/{lessonId}/completion

- 必须登录；没有请求体，不接受 userId。
- 第一次和重复调用都返回 200。
- LessonCompletionResponse：lessonId、completed = true、completedAt。
- 重复请求返回相同 completedAt。

### 7.5 GET /api/v1/me/courses/{courseId}/progress

- 必须登录，成功返回 200。
- CourseProgressResponse 至少包含 completedLessons、totalLessons、
  progressPercent、completedLessonIds、nextLesson。
- completedLessonIds 与完成数使用同一发布过滤和当前 userId，
  用于 Dashboard、CourseDetail 和 LessonView 恢复持久化完成状态。
- nextLesson 为 null，或包含 id、title、summary、estimatedMinutes、
  chapterId、chapterTitle。
- Course 未发布或不存在时返回 RESOURCE_NOT_FOUND。

## 8. 安全设计

Spring Security 只按 GET 方法精确 permitAll 公开课程读取：

- GET /api/v1/courses
- GET /api/v1/courses/{courseId}
- GET /api/v1/lessons/{lessonId}

现有 JwtAuthenticationFilter 在请求携带无效或过期 Bearer Token 时会直接返回 401，
而前端 Axios 会自动为请求附加会话 Token。因此，仅配置 permitAll 不足以保证匿名读取。
V0.3 实现时，JwtAuthenticationFilter 必须按请求方法和路径，
仅对上述三类精确公开 GET 跳过 JWT 认证解析；
即使请求携带无效或过期 Bearer Token，也按匿名请求继续。

/api/v1/me/** 不得跳过 JwtAuthenticationFilter，继续由 anyRequest().authenticated() 严格保护。
禁止使用覆盖 /api/v1/me/** 的宽泛公开匹配器。私有请求携带无效或过期 Token 时仍返回 401。

私有接口继续沿用 V0.2 最小 JWT、每请求数据库用户状态检查、统一 401/403、
显式 CORS、日志脱敏和参数化 SQL。
禁用或删除用户的旧 Token 不能访问进度；404 不清理认证状态，只有 401 触发现有退出流程。

现有 CORS allowedMethods 缺少 PUT；后续实现必须加入 PUT，
并验证允许来源和拒绝来源的预检。
不得使用通配 Origin 或 Cookie credentials。

## 9. 前端设计

### 9.1 路由与 HTTP

新增公开路由：

- /courses → CoursesView
- /courses/:courseId → CourseDetailView
- /lessons/:lessonId → LessonView

保留受保护的 /dashboard 和 /profile。
公开页面不设置 requiresAuth；会话恢复失败不能阻止匿名课程浏览。

继续使用现有 baseURL = /api/v1 的同一个 Axios 实例，
以及同一组 Token/401 拦截器。V0.3 课程 API 模块使用
/courses、/lessons/{lessonId} 和 /me/... 等相对路径，最终请求统一落在 /api/v1。
公开 GET 可能仍会附带 sessionStorage 中的 Token；其匿名可用性由
JwtAuthenticationFilter 的精确路径跳过规则保证。

相关 V0.2 API 测试必须回归，不修改全局 baseURL，
不创建第二套拦截器或响应结构。

### 9.2 状态管理

- AuthStore 继续是认证状态唯一来源。
- 课程 feature 使用明确 TypeScript 类型、集中 API 模块和组件本地状态。
- 不新增长期缓存课程数据的 Pinia Store。
- 页面显式处理 loading、empty、error 和 success。
- completion 成功后重新读取 progress，不由前端自行累加。

### 9.3 页面

CoursesView：

- 展示标题、摘要、可选封面和课程入口。
- 无课程时显示教育内容尚未发布的空状态。
- 匿名和登录用户看到相同公开内容。

CourseDetailView：

- 展示课程简介、有序 Chapter、Lesson 摘要和预计时间。
- 空 Chapter 显示空状态。
- 目录一次加载，不按 Chapter 单独发请求。
- 登录用户同时请求当前课程 progress，用 completedLessonIds 标记已完成课时。
- 匿名用户只请求公开课程详情，不请求私有 progress。

LessonView：

- 展示标题、摘要、预计时间和 Markdown 正文。
- 匿名用户看到登录提示，不显示完成按钮。
- 登录用户可幂等完成；pending 时禁用按钮。
- 登录用户根据 LessonDetail 返回的课程 ID 请求 progress，
  用 completedLessonIds.includes(lessonId) 在页面刷新后恢复已完成状态。
- 完成成功后重新读取 progress，立即更新已完成状态和 nextLesson。
- nextLesson 非空时显示继续学习；为 null 时显示课程完成。
- completedLessonIds 已覆盖单课完成状态恢复，V0.3 不新增单独的 lesson-completion 查询 endpoint。

Dashboard：

- 读取公开课程列表，选择排序第一的课程，不硬编码种子 ID；空列表不请求 progress。
- 消费 completedLessons、totalLessons、progressPercent、completedLessonIds 和 nextLesson，
  展示课程标题、completed / total、progressPercent、nextLesson 和继续学习。
- 100% 时显示课程完成。
- 保留个人中心和本地退出；不展示正确率、错题、AI、虚拟组合或图表。

### 9.4 Markdown

- 使用 markdown-it，配置 html: false。
- 只渲染 Flyway 中的受信任原创 Markdown。
- 不提供用户 Markdown 输入或富文本编辑器。
- 通过集中渲染组件进入 v-html。
- 保留 markdown-it 默认危险链接协议校验。
- 测试 script、事件 HTML 和危险协议链接不会生成可执行 DOM。

## 10. 测试与运行验证

后端单元/Web/Security 测试覆盖匿名读取、未发布 404、排序和发布过滤；
公开 GET 在无 Token 以及携带无效、过期 Bearer Token 时均可访问，
而 /api/v1/me/** 在同类 Token 下仍返回 401；
completion 认证与当前 userId、幂等且重复请求不改变首次 completedAt；
0%/部分/100%、向下取整、跳学、completedLessonIds、nextLesson；
用户隔离、精确 permitAll、PUT CORS，以及健康检查、OpenAPI 和 V0.2 认证回归。

真实 MySQL 8 验证 V1→V2→V3、种子数量与排序、四表结构；
唯一约束、并发 upsert 仅一行且首次时间不变；两用户 COUNT/nextLesson 隔离；
未发布过滤、目录无 N+1，并对关键查询执行 EXPLAIN。

前端覆盖四个页面及其加载/空/错误状态、排序和 Markdown 安全；
匿名/登录差异、completion pending/成功/401/404、nextLesson 和 100%；
Dashboard、CourseDetail 和 LessonView 刷新后重新请求 progress，
通过 completedLessonIds 恢复持久化完成状态；
公开/受保护路由、404 不退出及 V0.2 认证回归。

后续必须实际执行 mvn clean test、mvn clean package、npm ci、
npm run test:unit、npm run type-check 和 npm run build。

还必须用真实 HTTP/浏览器验证公开读取、登录完成、刷新后的进度、
用户隔离、未发布内容不可见和全部完成状态。
不为增加数量重复堆低价值测试。

## 11. 后续轻量 SDD 原则

后续实施按四个阶段组织：

1. Database + Course Read Model
2. Progress
3. Frontend Course
4. Dashboard + Integration

每阶段一个主要实现 Agent 加聚焦测试。
四阶段完成后统一执行一次 Review，集中修复，再做全量验证。
不恢复 V0.2 每个小任务多轮 implementer/reviewer/fixer 的重型流程。
本文档不规定逐文件任务顺序，也不替代 Implementation Plan。

## 12. 验收标准

1. 匿名用户可读取已发布课程、目录和课时；公开 GET 精确 permitAll 并跳过 JWT 解析。
2. 未发布 Course 或 Lesson 无法通过任何接口读取。
3. completion 与 progress 必须认证，userId 只来自 AuthenticatedUser。
4. 两个用户的进度隔离。
5. 第一次与重复完成均为 200、只有一行且首次 completed_at 不变。
6. 分母只含已发布 Lesson，0%、向下取整的部分完成和全部完成 100% 正确。
7. nextLesson 是排序第一的已发布未完成 Lesson；全部完成为 null。
8. 课程详情三次批量查询，不产生 Chapter 级 N+1。
9. Flyway 从空库创建四表和一门十章二十课时课程。
10. Markdown 原始 HTML 被关闭。
11. Dashboard 只包含课程进度，不提前实现 V0.6 聚合。
12. 全量测试、打包、类型检查、构建和运行验证有真实证据。
13. /api/v1/me/** 始终保持认证要求，公开 GET 的 JWT 跳过不得影响私有接口。
14. Dashboard、CourseDetail 和 LessonView 可通过 completedLessonIds 恢复持久化完成状态。
15. Git 无真实密钥、本地环境文件或范围外业务。
