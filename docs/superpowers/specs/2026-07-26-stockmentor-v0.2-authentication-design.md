# StockMentor V0.2 用户与认证模块设计规范

- 设计状态：已批准，待落库审阅
- 完成时间：2026-07-26 21:48:47
- 时区：Asia/Shanghai（UTC+8）
- 目标分支：`codex/v0.2-authentication`（已创建）
- 前置版本：V0.1 工程基线已完成并合并
- 本阶段不包含：课程、题库、AI 导师、公司分析、虚拟投资日志、正式仪表盘

---

## 1. 阶段目标

V0.2 建立完整身份认证闭环，为后续所有用户私有数据提供统一身份来源和隔离基础。

本阶段完成：

1. 用户表与 Flyway V2 迁移。
2. 邮箱密码注册。
3. 注册成功后自动登录。
4. 邮箱密码登录。
5. BCrypt 密码哈希。
6. JWT Access Token 的签发、解析和校验。
7. Spring Security 无状态认证过滤链。
8. 获取当前用户资料。
9. 修改当前用户昵称。
10. 用户状态和角色基础模型。
11. 前端登录、注册、个人资料和认证占位页。
12. Pinia 认证状态管理。
13. Axios 自动附加 Token。
14. 401 自动清理状态并跳转登录页。
15. 前端无状态退出。
16. 单元测试、Web 测试、集成测试和构建验证。

## 2. 明确不做的内容

V0.2 不实现：

- Refresh Token
- 邮箱验证码
- 找回密码
- 修改密码
- 头像上传
- OAuth 或第三方登录
- 多设备登录管理
- JWT 黑名单
- 登录失败锁定
- 管理员后台
- 用户删除账号
- 后端退出接口
- Redis 强制依赖
- 正式学习仪表盘统计
- 课程、题库和其他后续业务

这些内容需要单独设计，不得在 V0.2 中顺手加入。

## 3. 已批准的产品规则

### 3.1 登录状态存储

前端使用 `sessionStorage` 保存 JWT 和当前用户资料：

```text
stockmentor.accessToken
stockmentor.currentUser
```

刷新页面后保持登录；浏览器会话结束后不长期保留；不使用 `localStorage` 和 Cookie Session。

### 3.2 注册成功后的行为

注册成功后：

1. 后端创建用户。
2. 后端直接签发 JWT。
3. 返回统一认证响应。
4. 前端写入 `sessionStorage`。
5. Pinia 更新认证状态。
6. 自动跳转认证后的占位首页。

### 3.3 密码规则

- 长度 8–64 个字符。
- 至少包含一个英文字母。
- 至少包含一个数字。
- 允许常见特殊字符。
- 不强制同时包含大小写和特殊字符。
- 不自动去除首尾空格。
- 前后端执行一致校验。
- 数据库只保存 BCrypt 哈希。
- 日志、异常和响应中不得出现密码。
- V0.2 不实现连续失败锁定。

### 3.4 邮箱标准化

注册和登录时先去除首尾空格，再转换为小写，然后进行格式验证和数据库查询。

请求 DTO 只对邮箱是否提供做原始值检查；邮箱格式和 254 字符上限由可复用的 Service 层标准化组件在 `trim()` 和小写转换后统一验证，避免 Bean Validation 在标准化前拒绝合法输入。

以下输入视为同一账号：

```text
Test@Example.com
test@example.com
 test@example.com
```

不对 Gmail 点号和 `+` 别名做特殊合并。标准化后的邮箱在数据库中全局唯一。

### 3.5 昵称规则

- 注册时必填。
- 去除首尾空格。
- 长度 2–20 个字符。
- 不要求唯一。
- 允许中文、英文字母、数字、空格、下划线和短横线。
- 不允许纯空格。
- 登录后可以修改。
- 修改昵称不影响邮箱和 JWT 身份。
- V0.2 不实现头像。

请求 DTO 只对昵称是否提供做原始值检查；可复用的 Service 层昵称标准化组件负责先 `trim()`，再执行长度、字符集和非空校验。注册和昵称修改必须共用这一组件。

### 3.6 JWT 最小声明

JWT 只包含：

```json
{
  "sub": "用户ID",
  "iat": 生成时间,
  "exp": 过期时间,
  "jti": "随机唯一标识"
}
```

JWT 不包含邮箱、昵称、密码、用户状态、角色、学习进度或其他业务信息。

每个受保护请求根据用户 ID 查询数据库，并读取最新状态和角色。JWT 默认有效期为 7200 秒。

### 3.7 退出登录

V0.2 采用前端无状态退出：

1. 删除 `sessionStorage` 中的 Token。
2. 删除缓存用户资料。
3. 清空 Pinia。
4. 跳转登录页。

后端不维护 JWT 黑名单，也不创建退出接口。保留 `jti` 作为后续扩展基础。

## 4. 技术路线决策

采用：

> 最小 JWT 声明 + 每个受保护请求重新查询数据库用户状态

不采用把邮箱、昵称和角色写入 JWT，也不采用 Redis 会话 Token。后续只有在出现明确性能证据时，才考虑为用户状态读取增加缓存。

## 5. 数据库设计

新增迁移：

```text
stockmentor-backend/src/main/resources/db/migration/V2__create_user_and_auth_tables.sql
```

### 5.1 `sys_user` 表

```sql
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
```

### 5.2 字段和枚举

- `email`：只保存标准化后的邮箱。
- `password_hash`：只保存 BCrypt 字符串。
- `role`：`USER`、`ADMIN`。
- `status`：`ACTIVE`、`DISABLED`。
- `last_login_at`：注册自动登录和普通登录成功后更新。
- `deleted`：逻辑删除用户不得认证。

```java
public enum UserRole {
    USER,
    ADMIN
}
```

```java
public enum UserStatus {
    ACTIVE,
    DISABLED
}
```

### 5.3 并发重复注册

Service 层先查询标准化邮箱，数据库唯一索引 `uk_sys_user_email` 作为最终并发保护。唯一约束异常转换为 `USER_EMAIL_ALREADY_EXISTS`，HTTP 409。

## 6. 后端模块结构

建议新增：

```text
com.stockmentor
├── auth
│   ├── controller/AuthController
│   ├── dto/LoginRequest
│   ├── dto/RegisterRequest
│   ├── service/AuthenticationService
│   └── vo/AuthResponse
├── user
│   ├── controller/CurrentUserController
│   ├── domain/UserRole
│   ├── domain/UserStatus
│   ├── dto/UpdateNicknameRequest
│   ├── entity/UserEntity
│   ├── mapper/UserMapper
│   ├── repository/UserRepository
│   ├── repository/MyBatisUserRepository
│   ├── service/UserService
│   └── vo/CurrentUserResponse
└── infrastructure/security
    ├── AuthenticatedUser
    ├── JwtAuthenticationFilter
    ├── JwtProperties
    ├── JwtTokenProvider
    ├── RestAccessDeniedHandler
    ├── RestAuthenticationEntryPoint
    ├── SecurityUserService
    └── SecurityConfig
```

文件名可按现有工程习惯微调，但职责边界不得改变。

## 7. 关键职责

### `AuthController`

只负责请求接收、Bean Validation、调用 `AuthenticationService` 和返回统一响应。不得直接调用 Mapper、做 BCrypt、生成 JWT 或查询用户状态。

### `AuthenticationService`

负责邮箱标准化、注册规则、密码哈希、登录认证、账号状态判断、更新最后登录时间、JWT 签发和认证响应。

### `UserService`

负责查询当前用户、返回资料和修改昵称。

### `JwtTokenProvider`

只负责生成和校验 Token，不负责数据库查询。

### `JwtAuthenticationFilter`

流程：

1. 读取 `Authorization`。
2. 检查 `Bearer` 格式。
3. 验证 Token。
4. 读取用户 ID。
5. 查询数据库用户。
6. 确认存在、未删除且为 `ACTIVE`。
7. 读取最新角色。
8. 创建 `Authentication`。
9. 写入 `SecurityContextHolder`。
10. 继续过滤链。

失败统一返回 401，不暴露内部解析细节。

## 8. Spring Security 设计

公开接口：

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/system/health
GET  /v3/api-docs/**
GET  /swagger-ui/**
GET  /swagger-ui.html
```

其余 `/api/v1/**` 默认需要认证。

会话策略：

```java
SessionCreationPolicy.STATELESS
```

关闭 Form Login、HTTP Basic、默认生成密码和服务器 Session；保留 CORS、统一 401/403 和 JWT Filter。V0.1 的默认密码日志修复不得回退。

CORS 使用后端显式白名单：

- 允许来源从环境变量 `CORS_ALLOWED_ORIGINS` 读取，支持逗号分隔的多个来源。
- 本地开发默认仅允许 `http://localhost:5173`。
- 禁止通配来源 `*`。
- V0.2 只开放所需的 `GET`、`POST`、`PATCH` 和预检 `OPTIONS`，请求头只开放 `Authorization`、`Content-Type` 和 `Accept`。
- 因 Token 由 `Authorization` 请求头携带且不使用 Cookie，`allowCredentials` 保持 `false`。
- 必须用允许来源和拒绝来源的预检测试验证配置。

## 9. JWT 配置

```yaml
stockmentor:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration-seconds: ${JWT_EXPIRATION:7200}
```

要求：

- `JWT_SECRET` 只来自环境变量。
- 缺失或长度不足时启动明确失败。
- 不自动生成临时 JWT 密钥。
- 不打印密钥和完整 Token。
- 测试使用专用测试密钥。

## 10. API 设计

### 注册

```http
POST /api/v1/auth/register
```

请求：

```json
{
  "email": "student@example.com",
  "password": "study123",
  "nickname": "学习投资"
}
```

成功返回 HTTP 201：

```json
{
  "code": "SUCCESS",
  "message": "注册成功",
  "data": {
    "accessToken": "jwt-value",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "email": "student@example.com",
      "nickname": "学习投资",
      "role": "USER"
    }
  }
}
```

### 登录

```http
POST /api/v1/auth/login
```

成功返回 HTTP 200，数据结构与注册一致。失败统一返回：

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "邮箱或密码错误",
  "data": null
}
```

### 当前用户

```http
GET /api/v1/users/me
Authorization: Bearer <token>
```

返回 `id`、`email`、`nickname`、`role`、`createdAt`，不返回密码哈希、内部状态或数据库实体。

### 修改昵称

```http
PATCH /api/v1/users/me/nickname
Authorization: Bearer <token>
```

请求：

```json
{
  "nickname": "长期学习者"
}
```

返回更新后的当前用户资料。

### 退出

不创建后端退出接口，由前端清理认证状态。

## 11. 错误码设计

新增：

```text
AUTH_INVALID_CREDENTIALS
AUTH_INVALID_TOKEN
AUTH_TOKEN_EXPIRED
AUTH_USER_DISABLED
AUTH_USER_NOT_FOUND
USER_EMAIL_ALREADY_EXISTS
USER_NICKNAME_INVALID
```

| 场景 | HTTP | 对外错误码 |
|---|---:|---|
| 注册参数不合法 | 400 | `VALIDATION_FAILED` |
| 邮箱已注册 | 409 | `USER_EMAIL_ALREADY_EXISTS` |
| 邮箱或密码错误 | 401 | `AUTH_INVALID_CREDENTIALS` |
| Token 缺失或格式错误 | 401 | `AUTH_INVALID_TOKEN` |
| Token 过期 | 401 | `AUTH_TOKEN_EXPIRED` |
| Token 用户不存在、删除或禁用 | 401 | `AUTH_INVALID_TOKEN` |
| 已认证但角色不足 | 403 | 权限错误码 |
| 当前用户资料不存在 | 404 | `RESOURCE_NOT_FOUND` |

`AUTH_USER_DISABLED` 和 `AUTH_USER_NOT_FOUND` 可以用于服务端内部审计，对客户端统一为 `AUTH_INVALID_TOKEN`。

## 12. 事务设计

注册方法使用 `@Transactional`，事务包含邮箱检查、密码哈希、用户创建、保存和首次登录时间更新。首次登录时间更新失败时必须抛出异常并回滚用户写入；JWT 只能在全部数据库写入成功后签发。

登录的密码验证不使用长事务；仅更新 `last_login_at` 时使用必要短事务。

昵称修改使用短事务，仅修改当前用户记录。

## 13. 前端设计

建议结构：

```text
src/features/auth/
├── api/authApi.ts
├── stores/authStore.ts
├── types/auth.ts
└── views/
    ├── LoginView.vue
    └── RegisterView.vue

src/features/profile/
├── api/profileApi.ts
├── types/profile.ts
└── views/ProfileView.vue
```

Pinia 建议状态：

```ts
interface AuthState {
  accessToken: string | null
  currentUser: CurrentUser | null
  initialized: boolean
}
```

建议方法：

```text
register()
login()
restoreSession()
loadCurrentUser()
updateNickname()
logout()
clearSession()
```

应用启动时先恢复 `sessionStorage`，再调用 `/users/me` 验证 Token；完成后设置 `initialized`，避免刷新时错误跳转。

## 14. Axios 和路由

请求拦截器在存在 Token 时添加：

```text
Authorization: Bearer <token>
```

响应 401 时：

1. 清理 `sessionStorage`。
2. 清理 Pinia。
3. 防止并发 401 重复跳转。
4. 跳转 `/login`。
5. 保留原始 Axios 错误。

登录接口自身的 401 只显示“邮箱或密码错误”，不得形成重定向循环。

公共路由：

```text
/login
/register
```

受保护路由：

```text
/dashboard
/profile
```

未登录访问受保护路由跳转登录；已登录访问登录或注册页跳转占位仪表盘。V0.2 不实现正式仪表盘统计。

## 15. UI 范围

### 登录页

邮箱、密码、登录中状态、表单错误、注册链接和投资教育用途说明。

### 注册页

邮箱、昵称、密码、确认密码、规则提示和注册中状态。确认密码只在前端校验，不发送后端。

### 认证占位首页

显示欢迎昵称、邮箱、角色、V0.2 完成提示、个人中心入口和退出按钮，不显示学习统计。

### 个人中心

邮箱只读，昵称可编辑，提供保存反馈和退出入口。

## 16. 测试设计

后端必须覆盖：

- 邮箱标准化。
- 带首尾空格的合法邮箱和昵称在标准化后通过最终校验。
- 空值、非法格式和标准化后越界输入返回稳定业务错误。
- 重复邮箱冲突。
- BCrypt 哈希且不保存明文。
- 默认角色和状态。
- 注册自动登录。
- 正确和错误密码。
- 不存在邮箱与错误密码返回相同错误。
- 禁用和逻辑删除用户不能登录。
- JWT 包含 `sub`、`iat`、`exp`、`jti`。
- JWT 不包含邮箱、昵称和角色。
- 过期、篡改、错误密钥和缺失用户 ID 的 Token 被拒绝。
- 用户禁用或删除后旧 Token 返回 401。
- 获取当前用户和修改昵称。
- JSON 中没有 `passwordHash`。
- 注册 201、登录 200、重复邮箱 409、参数错误 400。
- 健康检查和 OpenAPI 仍公开。
- CORS 白名单允许配置来源并拒绝未配置来源。
- Flyway V2 从 V1 顺序执行。
- 首次登录时间更新失败时注册回滚且不签发 JWT。
- 启动日志不出现默认生成密码。

前端必须验证：

- 注册成功自动登录。
- 登录成功写入 `sessionStorage`。
- 刷新后恢复认证。
- 退出后全部清理。
- 401 自动退出。
- 登录失败不产生路由循环。
- 路由守卫正确。
- 昵称修改即时更新。
- `npm run type-check` 通过。
- `npm run build` 通过。

## 17. 安全要求

- 不提交真实 JWT 密钥和 `.env`。
- 不记录密码或完整 Token。
- 不暴露 `passwordHash`。
- 不泄露邮箱存在性。
- 不允许注册请求指定角色或状态。
- 不允许当前用户修改邮箱、角色或状态。
- 不回退 V0.1 默认密码日志和 `BusinessException` 安全修复。
- 前端校验不能替代后端校验。

## 18. 文档和变更记录

V0.2 完成时至少更新：

```text
docs/learning-notes/jwt-authentication.md
docs/learning-notes/spring-security-flow.md
docs/learning-notes/database-design.md
docs/learning-notes/transaction-design.md
docs/learning-notes/common-bugs.md
docs/learning-notes/interview-questions.md
README.md
```

并创建：

```text
docs/changelog/2026-07-xx-v0.2-authentication.md
```

Changelog 记录实际测试命令、测试数量、运行验证、密钥扫描、遗留问题和下一阶段。

## 19. 验收标准

V0.2 只有同时满足以下条件才可以接受：

1. 注册成功自动登录。
2. 邮箱标准化和唯一性正确。
3. 密码只保存 BCrypt 哈希。
4. 登录失败不泄露邮箱存在性。
5. JWT 只包含最小声明。
6. 过期、篡改和错误密钥 Token 被拒绝。
7. 用户禁用或删除后旧 Token 立即失效。
8. `/api/v1/users/me` 返回正确当前用户。
9. 昵称按规则修改。
10. 普通用户不能修改邮箱、角色或状态。
11. Token 保存在 `sessionStorage`。
12. 刷新后认证恢复。
13. 401 自动清理并跳转登录。
14. 登录失败不形成重定向循环。
15. 退出不依赖后端黑名单。
16. Flyway V2 从 V1 顺序执行。
17. 后端全量测试通过。
18. 后端打包通过。
19. 前端类型检查通过。
20. 前端生产构建通过。
21. 健康检查和 OpenAPI 正常。
22. 后端 CORS 只允许环境变量配置的显式来源。
23. 启动日志没有默认生成密码。
24. Git 中没有真实密码、JWT 密钥或 Token。
25. README、学习笔记、检查表和 changelog 更新。
26. 没有提前实现课程、题库或其他后续模块。

## 20. 最终设计结论

V0.2 正式采用：

- 邮箱密码认证
- 注册成功自动登录
- BCrypt
- JWT Access Token
- 两小时有效期
- 最小 JWT 声明
- 每个受保护请求查询数据库用户状态
- `sessionStorage`
- 前端无状态退出
- 用户状态即时失效
- USER/ADMIN 角色基础
- 无 Refresh Token
- 无邮箱验证
- 无 Redis 认证依赖
- 无管理员后台
- 测试驱动和阶段验收

该设计确认后，下一步是编写独立的 V0.2 实施计划，不直接开始编码。
