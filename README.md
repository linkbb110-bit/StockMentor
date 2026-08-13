# StockMentor

StockMentor 是一个面向投资初学者的股票与投资学习系统，同时作为 Java 后端实习项目使用。V0.3 课程与学习进度已完成实现，并于 2026-08-13 完成阶段验证。

## 产品边界

系统只用于投资教育与虚拟学习：

- 不提供具体买入或卖出建议。
- 不预测短期涨跌，不承诺收益。
- 不连接券商，不执行真实交易。
- 不支持杠杆、融资、期权或做空。

后续业务必须继续围绕知识、证据、风险与复盘展开，不能突破上述边界。

## 已验证环境

最终验证使用的实际工具链如下：

- Git `2.55.0.windows.3`
- JDK：Eclipse Temurin `17.0.19+10`
  - 本机 `PATH` 原先指向 Oracle Java `21.0.9`，后端验证显式使用 Java 17。
- Maven `3.9.16`
  - 路径：`C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd`
- Node.js `24.18.0`
- npm `11.16.0`
  - PowerShell 执行策略会阻止 `npm.ps1`，请使用 `npm.cmd` 或 `cmd.exe /d /c`。
- MySQL Community Server `8.4.7`
  - 路径：`C:\Program Files\MySQL\MySQL Server 8.4`
- Docker：未安装或不在 `PATH`，V0.3 不要求 Docker；最终验证直接使用隔离的 MySQL 8.4.7 实例完成。

主要受管依赖为 Spring Boot `3.5.16`、Spring Security OAuth2 JOSE、MyBatis-Plus `3.5.17`、Flyway `11.7.2`、MySQL Connector/J `9.7.0` 和 springdoc-openapi `2.8.17`。前端锁定的核心版本包括 Vue `3.5.40`、Vue Router `5.2.0`、Pinia `4.0.2`、Axios `1.18.1`、Element Plus `2.14.3`、ECharts `6.1.0`、Vite `8.1.5`、TypeScript `6.0.3` 与 vue-tsc `3.3.8`。

## 本地数据库准备

以下 SQL 仅用于创建本地开发数据库和最小权限账户。请替换占位密码，不要把真实密码提交到 Git：

```sql
CREATE DATABASE stockmentor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'stockmentor_app'@'127.0.0.1'
  IDENTIFIED BY '<请替换为本地强密码>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON stockmentor.*
  TO 'stockmentor_app'@'127.0.0.1';
```

本地配置通过环境变量传入。仓库只跟踪 `.env.example` 和 `application-local.yml.example`；真实 `.env`、`application-local.yml`、构建产物和运行日志均已忽略。

## 启动后端

先构建可执行 JAR，再使用 Java 17 和 `local` profile 启动：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3306'
$env:DB_NAME = 'stockmentor'
$env:DB_USERNAME = 'stockmentor_app'
$env:DB_PASSWORD = '<请替换为本地强密码>'
$env:FLYWAY_ENABLED = 'true'
$env:JWT_SECRET = '<至少 32 字节的本地随机密钥>'
$env:JWT_EXPIRATION = '7200'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'
$env:REDIS_ENABLED = 'false'
$env:AI_PROVIDER = 'mock'

& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd' `
  '-Dmaven.repo.local=C:\path\to\maven-cache' `
  -f stockmentor-backend\pom.xml clean package

& "$env:JAVA_HOME\bin\java.exe" `
  -jar stockmentor-backend\target\stockmentor-backend-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=local
```

`JWT_SECRET` 是认证后端的必需环境变量，少于 32 字节时应用会明确拒绝启动；仓库不会生成临时密钥。Redis 和真实 AI API Key 仍不是启动依赖。不要在命令历史、日志或 tracked 配置中保存真实密码、JWT 密钥或 API Key。

## 启动前端

```powershell
Set-Location stockmentor-frontend
& npm.cmd ci
& npm.cmd run dev
```

前端开发服务器默认由 Vite 提供。认证页为 `/register` 和 `/login`，公开课程页为 `/courses`、`/courses/:courseId` 和 `/lessons/:lessonId`，受保护页为 `/dashboard` 和 `/profile`。Token 与当前用户仅保存在当前浏览器会话的 `sessionStorage`；退出时本地清理，不调用后端黑名单或退出接口。可通过本地环境变量设置 API 基础地址，但不要提交真实本地 `.env`。

## V0.2 已实现范围

- Flyway V2 `sys_user` 用户表、标准化邮箱唯一约束、`USER`/`ADMIN` 角色和 `ACTIVE`/`DISABLED` 状态模型。
- 邮箱密码注册与登录；注册成功后自动登录。密码保持原值、不 trim，须同时满足 8–64 字符和不超过 72 个 UTF-8 字节，只以 BCrypt 哈希持久化；错误密码和不存在邮箱返回相同公共错误。
- 两小时 JWT Access Token；只含 `sub`、`iat`、`exp`、`jti`，每次受保护请求都重新读取数据库用户状态和最新角色。
- Spring Security 无状态过滤链、显式 CORS 白名单和统一 JSON 401/403。
- 当前用户资料和昵称修改；响应不返回密码哈希、内部状态或数据库实体。
- Vue 登录、注册、认证占位首页和个人资料页；Pinia 会话恢复、Axios Token 注入、按 Token 与内存 generation 归属隔离异步认证结果和 401 单次清理跳转，以及前端无状态退出。
- 保留 V0.1 健康检查、OpenAPI、统一响应、异常处理及无默认 Spring 密码日志基线。

V0.2 提供以下认证接口：

```text
POST  /api/v1/auth/register
POST  /api/v1/auth/login
GET   /api/v1/users/me
PATCH /api/v1/users/me/nickname
```

## V0.3 已实现范围

- Flyway V3 创建 `course`、`chapter`、`lesson`、`user_lesson_progress`，并提供原创的 1 门课程、10 章、20 课时初始内容。
- 匿名用户可读取已发布课程、按章节排序的课程目录和 Markdown 课时正文；公开 GET 即使携带无效或过期 Token 也按匿名访问处理。
- 登录用户可幂等标记课时完成并读取个人课程进度；响应包含完成数量、总数、后端计算的百分比、已完成课时 ID 和下一课，所有私有查询按当前用户隔离。
- Vue 提供课程列表、课程详情、课时阅读和 Markdown 安全渲染；完成状态可在刷新后从后端恢复。
- `/dashboard` 展示当前课程的 0%、部分和 100% 状态，并在存在 `nextLesson` 时提供继续学习入口；完成课程后不显示失效入口。

V0.3 提供以下课程与进度接口：

```text
GET /api/v1/courses
GET /api/v1/courses/{courseId}
GET /api/v1/lessons/{lessonId}
PUT /api/v1/me/lessons/{lessonId}/completion
GET /api/v1/me/courses/{courseId}/progress
```

题库、错题、AI 导师、公司分析、虚拟组合和后续聚合仪表盘等 V0.4+ 模块均未提前实现。

## 最终验证

2026-08-13 使用上述工具链实际完成：

```text
mvn clean test         -> exit 0，BUILD SUCCESS，221/221 测试通过
mvn clean package      -> exit 0，BUILD SUCCESS，221/221 测试通过并生成可执行 JAR
npm run test:unit      -> exit 0，14 个文件、82/82 测试通过
npm run type-check     -> exit 0
npm run build          -> exit 0
GET  /api/v1/system/health       -> HTTP 200
GET  /api/v1/courses             -> HTTP 200（匿名及无效 Token）
GET  /api/v1/courses/1           -> HTTP 200
GET  /api/v1/lessons/1           -> HTTP 200
PUT  /api/v1/me/lessons/1/completion -> HTTP 200（重复调用保持首次时间）
GET  /api/v1/me/courses/1/progress   -> HTTP 200
```

Flyway 已在全新、隔离的 MySQL 8.4.7 空数据库上依次应用 V1、V2、V3；数据库核对为 1 Course、10 Chapter、20 Lesson，且用户/课时联合唯一约束存在。真实 HTTP 与浏览器验证确认 0%→部分进度→100%、刷新恢复、下一课导航、用户隔离、未发布资源 404、私有接口匿名 401、CORS PUT 和完成幂等。验证后临时实例、监听器和数据目录均已清理，已安装的 `MySQL84` 服务保持运行。

## 文档入口

- [总体设计](docs/superpowers/specs/2026-07-25-stockmentor-design.md)
- [V0.2 认证设计](docs/superpowers/specs/2026-07-26-stockmentor-v0.2-authentication-design.md)
- [V0.2 实施计划](docs/superpowers/plans/2026-07-26-stockmentor-v0.2-authentication.md)
- [V0.3 课程与学习进度设计](docs/superpowers/specs/2026-08-09-stockmentor-v0.3-course-progress-design.md)
- [V0.3 实施计划](docs/superpowers/plans/2026-08-10-stockmentor-v0.3-course-progress.md)
- [Definition of Done](docs/06-development/definition-of-done.md)
- [V0.2 检查表](docs/06-development/phase-checklists/v0.2-authentication.md)
- [V0.2 最终变更记录](docs/changelog/2026-08-08-v0.2-authentication.md)
- [V0.3 变更记录](docs/changelog/2026-08-13-v0.3-course-progress.md)
