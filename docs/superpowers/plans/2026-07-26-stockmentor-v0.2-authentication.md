# StockMentor V0.2 Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 V0.1 工程基线上实现邮箱密码注册、注册后自动登录、JWT 无状态认证、当前用户资料、昵称修改以及对应的 Vue 认证流程。

**Architecture:** 后端继续使用模块化单体。认证入口位于 `auth` 模块，用户资料位于 `user` 模块，JWT 与 Spring Security 适配位于 `infrastructure/security`。JWT 只保存用户 ID、签发时间、过期时间和 `jti`；每个受保护请求重新查询数据库用户状态和最新角色。前端使用 Pinia 管理认证状态，使用 `sessionStorage` 保存当前浏览器会话，Axios 负责 Token 注入和失效处理。

**Tech Stack:** Java 17, Spring Boot 3.5.16, Spring Security, Spring Security OAuth2 JOSE, MyBatis-Plus 3.5.17, MySQL 8, Flyway 11, JUnit 5, Mockito, Vue 3.5, TypeScript 6, Vite 8, Pinia 4, Vue Router 5, Axios 1.18, Element Plus 2.14, Vitest, Vue Test Utils.

## Global Constraints

- 必须从已经合并 V0.1 的最新 `main` 创建 `codex/v0.2-authentication`。
- Java 必须使用 17；不要使用本机默认 Java 21 完成最终验证。
- 后端继续使用 Spring Boot 3.5.16 和 Maven。
- JWT 必须使用 Spring Security OAuth2 JOSE，不另外引入多套 JWT 库。
- JWT 只允许包含 `sub`、`iat`、`exp`、`jti`。
- JWT 默认有效期为 7200 秒。
- `JWT_SECRET` 必须来自环境变量；缺失或不足 32 字节时应用启动失败。
- 邮箱注册和登录前必须去除首尾空格并转换为小写。
- 密码为 8–64 字符，至少包含一个英文字母和一个数字，不得自动 `trim()`。
- 昵称去除首尾空格后必须为 2–20 字符，只允许中文、英文字母、数字、空格、下划线和短横线。
- 密码只允许以 BCrypt 哈希形式持久化。
- 登录失败不得区分邮箱不存在和密码错误。
- 普通注册请求不得接收角色和状态。
- 用户不得通过资料接口修改邮箱、角色或状态。
- 前端 Token 必须保存到 `sessionStorage`，不得改成 `localStorage`。
- V0.2 不实现 Refresh Token、验证码、找回密码、头像、JWT 黑名单、后端退出接口或管理员后台。
- V0.1 的默认密码日志和 `BusinessException` 安全修复不得回退。
- 不得提前实现课程、题库、AI、公司分析、虚拟组合或正式仪表盘。
- 每个任务先写失败测试，确认失败原因，再写最小实现。
- 每个任务测试通过后创建一次范围清晰的 Git commit。
- 每次阶段报告使用 Asia/Shanghai 时间，格式 `YYYY-MM-DD HH:mm:ss`。
- 未实际执行的测试、构建和运行验证不得声明通过。

---

## Repository Baseline

实施前确认以下 V0.1 事实仍成立：

- 后端工程：`stockmentor-backend`
- 前端工程：`stockmentor-frontend`
- 后端包名根：`com.stockmentor`
- 统一响应：`com.stockmentor.common.api.ApiResponse`
- 错误码：`com.stockmentor.common.exception.ErrorCode`
- 安全基线：`com.stockmentor.infrastructure.config.SecurityBaselineConfig`
- 健康接口：`GET /api/v1/system/health`
- OpenAPI：`GET /v3/api-docs`
- Flyway V1 已创建 `system_metadata`
- 前端 Axios：`stockmentor-frontend/src/api/http.ts`
- 前端路由：`stockmentor-frontend/src/router/index.ts`
- 前端 Pinia：`stockmentor-frontend/src/stores/index.ts`

不要假设文件与本计划完全一致。Task 1 必须先读取现有文件并记录差异；只有与认证目标直接相关的差异才允许纳入修改。

---

## Planned File Map

### Backend Dependencies and Configuration

- Modify: `stockmentor-backend/pom.xml`
  - 增加 `spring-security-oauth2-jose`
  - 增加测试期望所需依赖时保持最小化
- Modify: `stockmentor-backend/src/main/resources/application.yml`
  - 增加 JWT 配置
  - 保持 Redis 和 AI 可选
- Modify: `.env.example`
  - 保留不可用的 JWT 示例值
- Modify: `stockmentor-backend/src/main/resources/application-local.yml.example`
  - 展示从环境变量读取 JWT 配置
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtProperties.java`

### Database and User Persistence

- Create: `stockmentor-backend/src/main/resources/db/migration/V2__create_user_and_auth_tables.sql`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/domain/UserRole.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/domain/UserStatus.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/entity/UserEntity.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/mapper/UserMapper.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/repository/UserRepository.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/repository/MyBatisUserRepository.java`

### Authentication Domain

- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/dto/RegisterRequest.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/dto/LoginRequest.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/vo/AuthResponse.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/service/EmailNormalizer.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/service/AuthenticationService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/controller/AuthController.java`

### Security Infrastructure

- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/AuthenticatedUser.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtTokenProvider.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/SecurityUserService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtAuthenticationFilter.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/RestAuthenticationEntryPoint.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/RestAccessDeniedHandler.java`
- Replace or rename: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java`
  - 最终文件建议为 `SecurityConfig.java`

### Current User

- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/dto/UpdateNicknameRequest.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/vo/CurrentUserResponse.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/service/UserService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/controller/CurrentUserController.java`

### Backend Tests

- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/service/EmailNormalizerTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtTokenProviderTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/service/AuthenticationServiceTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtAuthenticationFilterTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/controller/AuthControllerTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/user/service/UserServiceTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/user/controller/CurrentUserControllerTest.java`
- Modify: `stockmentor-backend/src/test/java/com/stockmentor/StockMentorApplicationTests.java`
- Create: `stockmentor-backend/src/test/resources/application-test.yml` with a fixed non-production JWT test secret

### Frontend Authentication

- Create: `stockmentor-frontend/src/features/auth/types/auth.ts`
- Create: `stockmentor-frontend/src/features/auth/api/authApi.ts`
- Create: `stockmentor-frontend/src/features/auth/stores/authStore.ts`
- Create: `stockmentor-frontend/src/features/auth/views/LoginView.vue`
- Create: `stockmentor-frontend/src/features/auth/views/RegisterView.vue`
- Create: `stockmentor-frontend/src/features/auth/views/AuthDashboardView.vue`
- Create: `stockmentor-frontend/src/features/profile/api/profileApi.ts`
- Create: `stockmentor-frontend/src/features/profile/views/ProfileView.vue`
- Create: `stockmentor-frontend/src/features/auth/session/authSession.ts`
- Modify: `stockmentor-frontend/src/api/http.ts`
- Modify: `stockmentor-frontend/src/router/index.ts`
- Modify: `stockmentor-frontend/src/App.vue`
- Modify: `stockmentor-frontend/src/main.ts`
- Modify: `stockmentor-frontend/package.json`

### Frontend Tests

- Create: `stockmentor-frontend/vitest.config.ts`
- Create: `stockmentor-frontend/src/features/auth/session/authSession.spec.ts`
- Create: `stockmentor-frontend/src/features/auth/stores/authStore.spec.ts`
- Create: `stockmentor-frontend/src/router/routerGuards.spec.ts`

### Documentation

- Add: `docs/superpowers/specs/2026-07-26-stockmentor-v0.2-authentication-design.md`
- Add: `docs/superpowers/plans/2026-07-26-stockmentor-v0.2-authentication.md`
- Add: `docs/06-development/phase-checklists/v0.2-authentication.md`
- Add: `docs/changelog/2026-07-xx-v0.2-authentication.md` at final verification
- Modify: `README.md`
- Modify: relevant files under `docs/learning-notes`

---

### Task 1: Create the V0.2 Branch and Re-verify the V0.1 Baseline

**Files:**
- Create: `docs/superpowers/specs/2026-07-26-stockmentor-v0.2-authentication-design.md`
- Create: `docs/superpowers/plans/2026-07-26-stockmentor-v0.2-authentication.md`
- Create: `docs/06-development/phase-checklists/v0.2-authentication.md`
- Test: current backend and frontend full verification

**Interfaces:**
- Consumes: merged `main`
- Produces: clean `codex/v0.2-authentication` branch with accepted spec, plan and baseline evidence

- [ ] **Step 1: Synchronize the base branch**

```powershell
Set-Location "C:\Users\11707\Desktop\StockMentor-docs-v0.1"
git checkout main
git pull --ff-only origin main
git status
```

Expected: `main` is up to date and the worktree is clean.

- [ ] **Step 2: Create the feature branch**

```powershell
git checkout -b codex/v0.2-authentication
```

Expected: current branch is `codex/v0.2-authentication`.

- [ ] **Step 3: Read required project files**

Read in full:

```text
AGENTS.md
README.md
docs/superpowers/specs/2026-07-25-stockmentor-design.md
docs/superpowers/specs/2026-07-26-stockmentor-v0.2-authentication-design.md
docs/superpowers/plans/2026-07-26-stockmentor-v0.2-authentication.md
docs/06-development/coding-standards.md
docs/06-development/definition-of-done.md
stockmentor-backend/pom.xml
stockmentor-backend/src/main/resources/application.yml
stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java
stockmentor-frontend/package.json
stockmentor-frontend/src/api/http.ts
stockmentor-frontend/src/router/index.ts
stockmentor-frontend/src/main.ts
```

Expected: report any path or naming differences before modifying code.

- [ ] **Step 4: Run the unchanged backend baseline**

Use Java 17:

```powershell
$env:JAVA_HOME = 'C:\Users\11707\AppData\Local\Temp\stockmentor-temurin17\expanded\jdk-17.0.19+10'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -f stockmentor-backend\pom.xml clean test
```

Expected: V0.1 tests pass before V0.2 code changes.

- [ ] **Step 5: Run the unchanged frontend baseline**

```powershell
Set-Location stockmentor-frontend
& npm.cmd ci
& npm.cmd run type-check
& npm.cmd run build
Set-Location ..
```

Expected: type-check and build pass.

- [ ] **Step 6: Create the V0.2 checklist**

Use this exact initial checklist:

```markdown
# V0.2 用户与认证检查表

- [ ] 已从最新 main 创建 codex/v0.2-authentication
- [ ] V0.1 后端测试基线通过
- [ ] V0.1 前端 type-check 和 build 基线通过
- [ ] Flyway V2 已验证
- [ ] 注册成功自动登录
- [ ] 登录失败不泄露邮箱存在性
- [ ] 密码只保存 BCrypt 哈希
- [ ] JWT 仅包含 sub、iat、exp、jti
- [ ] 用户禁用或删除后旧 Token 失效
- [ ] 当前用户接口已验证
- [ ] 昵称修改已验证
- [ ] sessionStorage 认证状态已验证
- [ ] 401 自动退出已验证
- [ ] 后端全量测试通过
- [ ] 后端 package 通过
- [ ] 前端单元测试通过
- [ ] 前端 type-check 通过
- [ ] 前端 build 通过
- [ ] 运行时冒烟通过
- [ ] Secret scan 通过
- [ ] README 和学习文档已更新
- [ ] V0.2 changelog 已完成
```

- [ ] **Step 7: Commit the approved documentation**

```powershell
git add docs/superpowers/specs/2026-07-26-stockmentor-v0.2-authentication-design.md `
        docs/superpowers/plans/2026-07-26-stockmentor-v0.2-authentication.md `
        docs/06-development/phase-checklists/v0.2-authentication.md
git commit -m "docs: add v0.2 authentication design and plan"
```

---

### Task 2: Add JWT Dependency and Fail-fast Configuration

**Files:**
- Modify: `stockmentor-backend/pom.xml`
- Modify: `stockmentor-backend/src/main/resources/application.yml`
- Modify: `.env.example`
- Modify: `stockmentor-backend/src/main/resources/application-local.yml.example`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtProperties.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtPropertiesTest.java`

**Interfaces:**
- Consumes: Spring Boot configuration
- Produces: validated `JwtProperties(secret, expirationSeconds)` and Spring Security JOSE classes

- [ ] **Step 1: Add the managed JOSE dependency**

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

Do not add JJWT, Auth0 Java JWT or a second JWT implementation.

- [ ] **Step 2: Write failing JWT property tests**

Create tests that assert:

```java
assertThatThrownBy(() -> new JwtProperties("", 7200))
    .isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -> new JwtProperties("too-short", 7200))
    .isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -> new JwtProperties("a".repeat(32), 0))
    .isInstanceOf(IllegalArgumentException.class);

JwtProperties properties = new JwtProperties("a".repeat(32), 7200);
assertThat(properties.expirationSeconds()).isEqualTo(7200);
```

- [ ] **Step 3: Run the test and confirm failure**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=JwtPropertiesTest test
```

Expected: compilation failure because `JwtProperties` does not exist.

- [ ] **Step 4: Implement `JwtProperties`**

```java
package com.stockmentor.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockmentor.security.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds
) {
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero");
        }
    }
}
```

- [ ] **Step 5: Add application configuration**

Add:

```yaml
stockmentor:
  security:
    jwt:
      secret: ${JWT_SECRET:}
      expiration-seconds: ${JWT_EXPIRATION:7200}
```

Keep existing Redis and AI configuration.

- [ ] **Step 6: Update safe examples**

`.env.example`:

```dotenv
JWT_SECRET=replace-with-at-least-32-random-characters
JWT_EXPIRATION=7200
```

`application-local.yml.example` must reference environment variables, not contain a usable secret.

- [ ] **Step 7: Enable configuration properties**

Add `@EnableConfigurationProperties(JwtProperties.class)` to a focused security configuration class or the application class. Do not enable default Spring Security users.

- [ ] **Step 8: Run the property test**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=JwtPropertiesTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add stockmentor-backend/pom.xml `
        stockmentor-backend/src/main/resources/application.yml `
        stockmentor-backend/src/main/resources/application-local.yml.example `
        stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtProperties.java `
        stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtPropertiesTest.java `
        .env.example
git commit -m "feat: add validated jwt configuration"
```

---

### Task 3: Add Flyway V2 and User Persistence

**Files:**
- Create: `stockmentor-backend/src/main/resources/db/migration/V2__create_user_and_auth_tables.sql`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/domain/UserRole.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/domain/UserStatus.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/entity/UserEntity.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/mapper/UserMapper.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/repository/UserRepository.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/repository/MyBatisUserRepository.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/user/repository/MyBatisUserRepositoryTest.java`

**Interfaces:**
- Produces:
  - `Optional<UserEntity> findByNormalizedEmail(String email)`
  - `Optional<UserEntity> findActiveIdentityById(long userId)`
  - `UserEntity save(UserEntity user)`
  - `boolean updateLastLoginAt(long userId, LocalDateTime value)`
  - `boolean updateNickname(long userId, String nickname)`

- [ ] **Step 1: Create the Flyway V2 migration**

Use exactly:

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

- [ ] **Step 2: Define enums**

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

- [ ] **Step 3: Define `UserEntity`**

The entity must contain:

```java
private Long id;
private String email;
private String passwordHash;
private String nickname;
private UserRole role;
private UserStatus status;
private LocalDateTime lastLoginAt;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
@TableLogic
private Integer deleted;
```

Use MyBatis-Plus table and field mappings. Do not add password getters to any VO.

- [ ] **Step 4: Define repository contract**

```java
public interface UserRepository {
    Optional<UserEntity> findByNormalizedEmail(String normalizedEmail);
    Optional<UserEntity> findIdentityById(long userId);
    UserEntity save(UserEntity user);
    boolean updateLastLoginAt(long userId, LocalDateTime lastLoginAt);
    boolean updateNickname(long userId, String nickname);
}
```

`findIdentityById` must exclude logically deleted users. Status checking remains explicit in service/security code.

- [ ] **Step 5: Implement MyBatis repository**

`findByNormalizedEmail` must use an equality condition on `email`.
`findIdentityById` must use both `id` and `deleted = 0`.
`save` must populate generated ID.
Update methods must require both `id` and `deleted = 0`.

- [ ] **Step 6: Write repository tests**

Use `@ExtendWith(MockitoExtension.class)` and mock `UserMapper` to verify the repository wrapper without claiming database compatibility:

- `save` invokes `insert`, then returns the entity containing the mapper-populated ID;
- normalized email lookup builds an equality query for `email`;
- identity lookup includes the requested `id` and relies on MyBatis-Plus logical-delete filtering;
- nickname update targets only the requested ID;
- last-login update writes the exact timestamp.

Do not introduce H2. Real MySQL schema, indexes, generated IDs and queries are verified in Task 11.

- [ ] **Step 7: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=MyBatisUserRepositoryTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add stockmentor-backend/src/main/resources/db/migration/V2__create_user_and_auth_tables.sql `
        stockmentor-backend/src/main/java/com/stockmentor/user `
        stockmentor-backend/src/test/java/com/stockmentor/user/repository
git commit -m "feat: add user persistence model"
```

---

### Task 4: Implement Input Normalization, DTO Validation and Error Codes

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/service/EmailNormalizer.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/dto/RegisterRequest.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/dto/LoginRequest.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/dto/UpdateNicknameRequest.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/common/exception/ErrorCode.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/service/EmailNormalizerTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/dto/AuthRequestValidationTest.java`

**Interfaces:**
- Produces:
  - `String EmailNormalizer.normalize(String rawEmail)`
  - validated request records
  - stable authentication and user error codes

- [ ] **Step 1: Write email normalization tests**

```java
@Test
void shouldTrimAndLowercaseEmail() {
    assertThat(normalizer.normalize(" Test@Example.COM "))
        .isEqualTo("test@example.com");
}

@Test
void shouldRejectNullEmail() {
    assertThatThrownBy(() -> normalizer.normalize(null))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Run and confirm failure**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=EmailNormalizerTest test
```

- [ ] **Step 3: Implement `EmailNormalizer`**

```java
@Component
public class EmailNormalizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@" +
        "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
        "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    public String normalize(String rawEmail) {
        if (rawEmail == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return normalized;
    }
}
```

- [ ] **Step 4: Define request DTOs**

`RegisterRequest`:

```java
public record RegisterRequest(
    @NotBlank @Size(max = 254) @Email String email,
    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$")
    String password,
    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9 _-]+$")
    String nickname
) {}
```

`LoginRequest` uses the same email and password length rules but must not enforce password composition again if that would prevent an existing valid account from attempting login. Use:

```java
public record LoginRequest(
    @NotBlank @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 64) String password
) {}
```

`UpdateNicknameRequest` uses the same nickname constraints as registration.

The service must normalize nickname before final business validation, because Bean Validation runs before trimming.

- [ ] **Step 5: Extend `ErrorCode`**

Add at least:

```java
AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "邮箱或密码错误"),
AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录"),
AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"),
AUTH_USER_DISABLED(HttpStatus.UNAUTHORIZED, "用户不可用"),
AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "用户不存在"),
USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "该邮箱已注册"),
USER_NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "昵称格式不合法"),
FORBIDDEN(HttpStatus.FORBIDDEN, "没有权限执行该操作");
```

Only safe public codes are returned to clients.

- [ ] **Step 6: Test DTO validation**

Use Jakarta Validator to verify:

- password with letters and digits passes;
- letters-only password fails registration;
- digits-only password fails registration;
- 65-character password fails;
- pure-space nickname fails after service normalization;
- Chinese nickname passes;
- underscore and hyphen pass.

- [ ] **Step 7: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=EmailNormalizerTest,AuthRequestValidationTest test
```

- [ ] **Step 8: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/auth `
        stockmentor-backend/src/main/java/com/stockmentor/user/dto `
        stockmentor-backend/src/main/java/com/stockmentor/common/exception/ErrorCode.java `
        stockmentor-backend/src/test/java/com/stockmentor/auth
git commit -m "feat: add authentication input rules"
```

---

### Task 5: Implement Minimal JWT Issuing and Parsing

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtTokenProvider.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtTokenProviderTest.java`

**Interfaces:**
- Produces:
  - `String issue(long userId, Instant issuedAt)`
  - `long parseUserId(String token)`
  - `long expirationSeconds()`

- [ ] **Step 1: Write failing tests**

Tests must verify:

```java
String token = provider.issue(42L, Instant.parse("2026-07-26T12:00:00Z"));
Jwt decoded = decoder.decode(token);

assertThat(decoded.getSubject()).isEqualTo("42");
assertThat(decoded.getIssuedAt()).isEqualTo(Instant.parse("2026-07-26T12:00:00Z"));
assertThat(decoded.getExpiresAt()).isEqualTo(Instant.parse("2026-07-26T14:00:00Z"));
assertThat(decoded.getId()).isNotBlank();
assertThat(decoded.getClaims()).doesNotContainKeys("email", "nickname", "role");
```

Also verify:

- invalid signature throws an authentication-specific exception;
- expired Token maps to `AUTH_TOKEN_EXPIRED`;
- non-numeric `sub` maps to `AUTH_INVALID_TOKEN`;
- altered Token is rejected.

- [ ] **Step 2: Run and confirm failure**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=JwtTokenProviderTest test
```

- [ ] **Step 3: Implement the provider**

Use `NimbusJwtEncoder` and `NimbusJwtDecoder` with HS256 and a `SecretKeySpec`.

Core issuing logic:

```java
public String issue(long userId, Instant issuedAt) {
    Instant expiresAt = issuedAt.plusSeconds(properties.expirationSeconds());
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .subject(Long.toString(userId))
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .id(UUID.randomUUID().toString())
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
}
```

Core parsing logic:

```java
public long parseUserId(String token) {
    try {
        Jwt jwt = decoder.decode(token);
        return Long.parseLong(jwt.getSubject());
    } catch (JwtValidationException ex) {
        boolean expired = ex.getErrors().stream()
            .anyMatch(error -> error.getDescription().toLowerCase(Locale.ROOT).contains("expired"));
        throw new BusinessException(
            expired ? ErrorCode.AUTH_TOKEN_EXPIRED : ErrorCode.AUTH_INVALID_TOKEN
        );
    } catch (RuntimeException ex) {
        throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
}
```

Do not log the token.

- [ ] **Step 4: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=JwtTokenProviderTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtTokenProvider.java `
        stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtTokenProviderTest.java
git commit -m "feat: add minimal jwt token provider"
```

---

### Task 6: Implement Registration and Login Service

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/vo/AuthResponse.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/vo/CurrentUserResponse.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/service/AuthenticationService.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/service/AuthenticationServiceTest.java`
- Modify: security configuration to expose a BCrypt `PasswordEncoder` bean

**Interfaces:**
- Produces:
  - `AuthResponse register(RegisterRequest request)`
  - `AuthResponse login(LoginRequest request)`
  - `CurrentUserResponse` without password or internal status

- [ ] **Step 1: Define response types**

```java
public record CurrentUserResponse(
    Long id,
    String email,
    String nickname,
    UserRole role,
    LocalDateTime createdAt
) {}
```

```java
public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    CurrentUserResponse user
) {
    public static AuthResponse bearer(
            String token,
            long expiresIn,
            CurrentUserResponse user
    ) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
```

- [ ] **Step 2: Write registration tests**

Using Mockito, verify:

- `" Test@Example.com "` is queried and saved as `test@example.com`;
- duplicate email throws `USER_EMAIL_ALREADY_EXISTS`;
- saved `passwordHash` is not equal to plaintext;
- `passwordEncoder.matches(plaintext, savedHash)` is true;
- role is `USER`;
- status is `ACTIVE`;
- `lastLoginAt` is set;
- token is issued after a user ID exists;
- returned response contains no password field.

- [ ] **Step 3: Write login tests**

Verify:

- existing ACTIVE user with correct password succeeds;
- wrong password throws `AUTH_INVALID_CREDENTIALS`;
- missing email throws the same error;
- DISABLED user does not receive a Token;
- deleted user is treated as unavailable by repository lookup;
- successful login updates `lastLoginAt`;
- error messages do not reveal email existence.

- [ ] **Step 4: Run and confirm failure**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=AuthenticationServiceTest test
```

- [ ] **Step 5: Implement BCrypt bean**

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- [ ] **Step 6: Implement registration**

Required order:

1. normalize email;
2. trim and validate nickname;
3. check duplicate;
4. hash password;
5. construct USER/ACTIVE entity;
6. save;
7. update or populate first login time;
8. issue Token;
9. map to `AuthResponse`.

Catch database duplicate-key exceptions and convert them to `USER_EMAIL_ALREADY_EXISTS`.

- [ ] **Step 7: Implement login**

Required behavior:

```java
UserEntity user = repository.findByNormalizedEmail(normalizedEmail)
    .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

if (user.getStatus() != UserStatus.ACTIVE
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
    throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
}
```

Update `lastLoginAt`, issue Token and return response.

Do not log the email-password combination or distinguish failure reasons in the client response.

- [ ] **Step 8: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=AuthenticationServiceTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/auth `
        stockmentor-backend/src/main/java/com/stockmentor/user/vo `
        stockmentor-backend/src/test/java/com/stockmentor/auth/service `
        stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config
git commit -m "feat: implement registration and login service"
```

---

### Task 7: Implement Database-backed Security Identity

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/AuthenticatedUser.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/SecurityUserService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtAuthenticationFilter.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/SecurityUserServiceTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/JwtAuthenticationFilterTest.java`

**Interfaces:**
- Produces:
  - `AuthenticatedUser(long userId, String email, UserRole role)`
  - `AuthenticatedUser SecurityUserService.load(long userId)`
  - one authentication filter per request

- [ ] **Step 1: Define `AuthenticatedUser`**

```java
public record AuthenticatedUser(
    long userId,
    String email,
    UserRole role
) {}
```

- [ ] **Step 2: Write `SecurityUserService` tests**

Verify:

- ACTIVE user becomes `AuthenticatedUser`;
- DISABLED user throws internal disabled classification;
- missing/deleted user throws internal not-found classification;
- role is read from the latest database entity.

- [ ] **Step 3: Implement `SecurityUserService`**

```java
public AuthenticatedUser load(long userId) {
    UserEntity user = repository.findIdentityById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    if (user.getStatus() != UserStatus.ACTIVE) {
        throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);
    }

    return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
}
```

The filter/entry point will map internal user state failures to public `AUTH_INVALID_TOKEN`.

- [ ] **Step 4: Write filter tests**

Verify:

- no Authorization header leaves SecurityContext empty;
- non-Bearer header yields public 401;
- valid Token loads user and writes Authentication;
- disabled user yields public 401;
- malformed Token yields public 401;
- filter does not log or expose the Token.

- [ ] **Step 5: Implement filter**

Use `OncePerRequestFilter`.

Authority format:

```java
new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name())
```

Principal must be the `AuthenticatedUser`, not the raw JWT string.

- [ ] **Step 6: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml `
  -Dtest=SecurityUserServiceTest,JwtAuthenticationFilterTest test
```

- [ ] **Step 7: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security `
        stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security
git commit -m "feat: add database-backed jwt authentication"
```

---

### Task 8: Add Unified 401/403 Handling and Final Security Chain

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/RestAuthenticationEntryPoint.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/RestAccessDeniedHandler.java`
- Replace or modify: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/infrastructure/security/SecurityConfigTest.java`

**Interfaces:**
- Produces:
  - unified JSON 401 using `AUTH_INVALID_TOKEN`
  - unified JSON 403 using `FORBIDDEN`
  - public auth, health and OpenAPI routes
  - protected remaining API routes

- [ ] **Step 1: Write Web security tests**

Verify:

```text
POST /api/v1/auth/register -> not blocked by security
POST /api/v1/auth/login    -> not blocked by security
GET /api/v1/system/health  -> 200
GET /v3/api-docs           -> 200
GET /api/v1/users/me       -> 401 without Token
```

Assert 401 JSON:

```json
{
  "code": "AUTH_INVALID_TOKEN",
  "message": "登录状态已失效，请重新登录",
  "data": null
}
```

- [ ] **Step 2: Implement handlers**

Serialize `ApiResponse.failure(...)` through the configured Jackson `ObjectMapper`.
Set content type to UTF-8 JSON and set the correct HTTP status before writing.

- [ ] **Step 3: Replace baseline security chain**

Final rules:

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .formLogin(AbstractHttpConfigurer::disable)
    .httpBasic(AbstractHttpConfigurer::disable)
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .exceptionHandling(errors -> errors
        .authenticationEntryPoint(authenticationEntryPoint)
        .accessDeniedHandler(accessDeniedHandler))
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers(HttpMethod.POST,
            "/api/v1/auth/register",
            "/api/v1/auth/login").permitAll()
        .requestMatchers(
            "/api/v1/system/health",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

- [ ] **Step 4: Ensure no default user/password is created**

Keep the V0.1 exclusion or configuration that prevents default password logs. In `StockMentorApplicationTests`, add an explicit assertion that the context contains no `UserDetailsServiceAutoConfiguration`-created default user bean and retain the existing captured-log assertion whose expected generated-password match count is zero.

- [ ] **Step 5: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=SecurityConfigTest,*ApplicationTests test
```

- [ ] **Step 6: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/infrastructure `
        stockmentor-backend/src/test/java/com/stockmentor/infrastructure `
        stockmentor-backend/src/test/java/com/stockmentor/StockMentorApplicationTests.java
git commit -m "feat: secure api with unified jwt handling"
```

---

### Task 9: Add Authentication HTTP Endpoints

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/auth/controller/AuthController.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/auth/controller/AuthControllerTest.java`
- Modify: `stockmentor-backend/src/main/java/com/stockmentor/common/exception/GlobalExceptionHandler.java` to map `DuplicateKeyException` and `DataIntegrityViolationException` caused by `uk_sys_user_email` to HTTP 409 with `USER_EMAIL_ALREADY_EXISTS`

**Interfaces:**
- Produces:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`

- [ ] **Step 1: Write register endpoint tests**

Verify:

- valid request returns HTTP 201;
- response message is `注册成功`;
- response includes `accessToken`, `tokenType`, `expiresIn`, user fields;
- response does not include `password`, `passwordHash`, `status`, `deleted`;
- invalid password returns 400;
- invalid nickname returns 400;
- duplicate email returns 409.

- [ ] **Step 2: Write login endpoint tests**

Verify:

- valid request returns HTTP 200;
- invalid credentials return 401;
- nonexistent email and wrong password return identical public code and message;
- password never appears in response.

- [ ] **Step 3: Implement controller**

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("注册成功",
                authenticationService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success("登录成功",
            authenticationService.login(request));
    }
}
```

- [ ] **Step 4: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml -Dtest=AuthControllerTest test
```

- [ ] **Step 5: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/auth/controller `
        stockmentor-backend/src/test/java/com/stockmentor/auth/controller `
        stockmentor-backend/src/main/java/com/stockmentor/common/exception
git commit -m "feat: expose registration and login api"
```

---

### Task 10: Add Current User and Nickname APIs

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/service/UserService.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/user/controller/CurrentUserController.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/user/service/UserServiceTest.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/user/controller/CurrentUserControllerTest.java`

**Interfaces:**
- Produces:
  - `CurrentUserResponse getCurrentUser(long userId)`
  - `CurrentUserResponse updateNickname(long userId, UpdateNicknameRequest request)`
  - `GET /api/v1/users/me`
  - `PATCH /api/v1/users/me/nickname`

- [ ] **Step 1: Write service tests**

Verify:

- current user is mapped without password;
- nickname is trimmed;
- pure-space nickname returns `USER_NICKNAME_INVALID`;
- illegal character nickname returns validation failure;
- update uses current authenticated user ID;
- repository update failure returns resource-not-found.

- [ ] **Step 2: Implement `UserService`**

```java
public CurrentUserResponse getCurrentUser(long userId) {
    UserEntity user = repository.findIdentityById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    return toResponse(user);
}
```

Nickname logic must normalize before update and then read the latest record.

- [ ] **Step 3: Write controller tests**

Verify:

- valid authenticated principal returns 200;
- no authentication returns 401;
- PATCH updates nickname;
- request cannot contain or modify email, role or status;
- JSON does not expose internal fields.

- [ ] **Step 4: Implement controller**

Use:

```java
@AuthenticationPrincipal AuthenticatedUser currentUser
```

Endpoints:

```java
@GetMapping("/me")
public ApiResponse<CurrentUserResponse> me(...)

@PatchMapping("/me/nickname")
public ApiResponse<CurrentUserResponse> updateNickname(...)
```

- [ ] **Step 5: Run tests**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml `
  -Dtest=UserServiceTest,CurrentUserControllerTest test
```

- [ ] **Step 6: Commit**

```powershell
git add stockmentor-backend/src/main/java/com/stockmentor/user `
        stockmentor-backend/src/test/java/com/stockmentor/user
git commit -m "feat: add current user profile api"
```

---

### Task 11: Run Real MySQL Authentication Integration

**Files:**
- Create: `.superpowers/sdd/2026-07-26-stockmentor-v0.2-authentication/v0.2-runtime-report.md` as an ignored evidence file
- Modify: no tracked source file during this task unless a failing command first reproduces a defect and the defect is fixed under a new focused test
- Test: Flyway V1 + V2 and real HTTP authentication flow

**Interfaces:**
- Consumes: completed backend
- Produces: real MySQL evidence for migration, persistence, JWT and endpoint behavior

- [ ] **Step 1: Run all backend tests before runtime verification**

```powershell
$env:JAVA_HOME = 'C:\Users\11707\AppData\Local\Temp\stockmentor-temurin17\expanded\jdk-17.0.19+10'
& mvn.cmd -f stockmentor-backend\pom.xml clean test
```

Expected: PASS.

- [ ] **Step 2: Build the executable JAR**

```powershell
& mvn.cmd -f stockmentor-backend\pom.xml clean package
```

Expected: PASS.

- [ ] **Step 3: Start an isolated MySQL 8 instance**

Use the same safe temporary-datadir method proven in V0.1:

- port `3307`;
- datadir directly under the resolved OS temp directory;
- random directory name beginning with `stockmentor-mysql84-v02-`;
- validate parent and prefix before deletion;
- do not stop or reconfigure installed `MySQL84`;
- record start PID and listener PID.

- [ ] **Step 4: Create an empty database and least-privilege local user**

Create:

```sql
CREATE DATABASE stockmentor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

Create a temporary test user with a random password. Keep the password only in process environment and clear it during cleanup.

- [ ] **Step 5: Start the backend with explicit environment variables**

Generate process-local values and set them without writing any tracked file:

```powershell
$temporaryDbUser = 'stockmentor_v02'
$temporaryDbPassword = ([guid]::NewGuid().ToString('N') + 'Aa9!')
$temporaryJwtSecret = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))

$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3307'
$env:DB_NAME = 'stockmentor'
$env:DB_USERNAME = $temporaryDbUser
$env:DB_PASSWORD = $temporaryDbPassword
$env:FLYWAY_ENABLED = 'true'
$env:JWT_SECRET = $temporaryJwtSecret
$env:JWT_EXPIRATION = '7200'
$env:REDIS_ENABLED = 'false'
$env:AI_PROVIDER = 'mock'
```

Use the same generated database username and password when creating the temporary MySQL account. Clear these variables in `finally`.

- [ ] **Step 6: Verify Flyway**

Query:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected:

```text
1  create schema history baseline  1
2  create user and auth tables     1
```

Verify `sys_user`, unique email index and status index exist.

- [ ] **Step 7: Verify the complete HTTP flow**

Register:

```powershell
$registerBody = @{
  email = ' Student@Example.com '
  password = 'study123'
  nickname = '学习投资'
} | ConvertTo-Json

$register = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/auth/register' `
  -ContentType 'application/json' `
  -Body $registerBody
```

Assert:

- success response;
- normalized email is `student@example.com`;
- Token exists;
- token type is Bearer;
- expiresIn is 7200.

Use the returned Token:

```powershell
$headers = @{ Authorization = "Bearer $($register.data.accessToken)" }
Invoke-RestMethod -Headers $headers `
  -Uri 'http://localhost:8080/api/v1/users/me'
```

Update nickname and verify latest response.

Login with lowercase email and the correct password; verify success.

Login with wrong password and nonexistent email; verify both public responses are identical.

- [ ] **Step 8: Verify database security**

Query `sys_user` and assert:

- email is lowercase and trimmed;
- `password_hash` does not equal `study123`;
- hash begins with a BCrypt-compatible prefix;
- role is USER;
- status is ACTIVE;
- last_login_at is not null.

Never print the full password hash in the final public report.

- [ ] **Step 9: Verify Token content**

Decode header/payload locally without sharing the Token. Assert claims contain only standard header fields and:

```text
sub
iat
exp
jti
```

Assert claims do not contain:

```text
email
nickname
role
status
password
```

- [ ] **Step 10: Verify status invalidation**

Update the test user to `DISABLED` directly in the temporary database. Reuse the previously issued Token.

Expected: `/api/v1/users/me` returns 401 with public `AUTH_INVALID_TOKEN`.

- [ ] **Step 11: Clean up safely**

Stop only the backend and temporary MySQL processes started by this task.
Verify ports `8080` and `3307` have no listeners.
Verify installed `MySQL84` remains `Running`.
Delete only the validated temporary datadir.
Clear password and secret environment variables.

- [ ] **Step 12: Do not commit runtime secrets or logs**

Only summarize non-sensitive evidence in the final changelog.

---

### Task 12: Add Frontend Auth Types, Session Adapter and API Clients

**Files:**
- Modify: `stockmentor-frontend/package.json`
- Create: `stockmentor-frontend/vitest.config.ts`
- Create: `stockmentor-frontend/src/features/auth/types/auth.ts`
- Create: `stockmentor-frontend/src/features/auth/session/authSession.ts`
- Create: `stockmentor-frontend/src/features/auth/session/authSession.spec.ts`
- Create: `stockmentor-frontend/src/features/auth/api/authApi.ts`
- Create: `stockmentor-frontend/src/features/profile/api/profileApi.ts`

**Interfaces:**
- Produces:
  - typed auth API functions
  - centralized session storage adapter
  - frontend unit test command

- [ ] **Step 1: Add frontend testing dependencies and script**

Run:

```powershell
Set-Location stockmentor-frontend
& npm.cmd install --save-dev vitest @vue/test-utils jsdom
```

Add:

```json
"test:unit": "vitest run"
```

Do not remove existing scripts.

- [ ] **Step 2: Define auth types**

```ts
export interface CurrentUser {
  id: number
  email: string
  nickname: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: CurrentUser
}

export interface RegisterPayload {
  email: string
  password: string
  nickname: string
}

export interface LoginPayload {
  email: string
  password: string
}
```

- [ ] **Step 3: Write session adapter tests**

Verify:

- save writes exactly `stockmentor.accessToken` and `stockmentor.currentUser`;
- load returns parsed state;
- malformed user JSON is cleared safely;
- clear removes both keys;
- adapter never uses `localStorage`.

- [ ] **Step 4: Implement session adapter**

Export:

```ts
export const authSession = {
  load(): { accessToken: string | null; currentUser: CurrentUser | null },
  save(accessToken: string, currentUser: CurrentUser): void,
  clear(): void,
}
```

- [ ] **Step 5: Implement API clients**

`authApi.ts`:

```ts
export const register = (payload: RegisterPayload) =>
  http.post<ApiResponse<AuthResponse>>('/auth/register', payload)

export const login = (payload: LoginPayload) =>
  http.post<ApiResponse<AuthResponse>>('/auth/login', payload)
```

`profileApi.ts`:

```ts
export const getCurrentUser = () =>
  http.get<ApiResponse<CurrentUser>>('/users/me')

export const updateNickname = (nickname: string) =>
  http.patch<ApiResponse<CurrentUser>>('/users/me/nickname', { nickname })
```

- [ ] **Step 6: Run unit tests and type check**

```powershell
& npm.cmd run test:unit
& npm.cmd run type-check
```

- [ ] **Step 7: Commit**

```powershell
Set-Location ..
git add stockmentor-frontend/package.json `
        stockmentor-frontend/package-lock.json `
        stockmentor-frontend/vitest.config.ts `
        stockmentor-frontend/src/features
git commit -m "feat: add frontend authentication clients"
```

---

### Task 13: Implement Pinia Authentication State

**Files:**
- Create: `stockmentor-frontend/src/features/auth/stores/authStore.ts`
- Create: `stockmentor-frontend/src/features/auth/stores/authStore.spec.ts`

**Interfaces:**
- Produces:
  - `useAuthStore`
  - `register`, `login`, `restoreSession`, `loadCurrentUser`, `updateNickname`, `logout`, `clearSession`

- [ ] **Step 1: Write store tests**

Verify:

- register saves Token and user;
- login saves Token and user;
- restore reads session and validates with `/users/me`;
- invalid restored Token clears state;
- nickname update updates store and session;
- logout clears store and session;
- initialized becomes true after restore finishes.

- [ ] **Step 2: Implement store**

State:

```ts
state: () => ({
  accessToken: null as string | null,
  currentUser: null as CurrentUser | null,
  initialized: false,
})
```

Getters:

```ts
isAuthenticated: (state) => Boolean(state.accessToken && state.currentUser)
```

`register` and `login` must call the API, save the returned session, and update state.

`restoreSession` must:

1. load session;
2. set provisional Token;
3. call `loadCurrentUser`;
4. clear on failure;
5. set `initialized = true` in `finally`.

`logout` is local only.

- [ ] **Step 3: Run store tests**

```powershell
Set-Location stockmentor-frontend
& npm.cmd run test:unit -- authStore.spec.ts
```

- [ ] **Step 4: Commit**

```powershell
Set-Location ..
git add stockmentor-frontend/src/features/auth/stores
git commit -m "feat: add pinia authentication state"
```

---

### Task 14: Add Axios Authentication Interceptors and Router Guards

**Files:**
- Modify: `stockmentor-frontend/src/api/http.ts`
- Modify: `stockmentor-frontend/src/router/index.ts`
- Modify: `stockmentor-frontend/src/main.ts`
- Create: `stockmentor-frontend/src/router/routerGuards.spec.ts`

**Interfaces:**
- Consumes: `useAuthStore`, `authSession`
- Produces: Bearer injection, safe 401 handling and route protection

- [ ] **Step 1: Write interceptor and router behavior tests**

Verify:

- request with Token adds `Authorization: Bearer <token>`;
- request without Token does not add header;
- protected route without authentication redirects to `/login`;
- authenticated user visiting `/login` redirects to `/dashboard`;
- route guard waits for `restoreSession`;
- login endpoint 401 does not cause an infinite redirect loop.

- [ ] **Step 2: Implement request interceptor**

Use the session adapter or store access that does not create circular imports:

```ts
http.interceptors.request.use((config) => {
  const { accessToken } = authSession.load()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})
```

- [ ] **Step 3: Implement response interceptor**

For 401:

- do not repeatedly redirect when already on `/login`;
- preserve the original Axios error;
- clear session and store for expired authenticated requests;
- allow `/auth/login` invalid-credential errors to be handled by the login page.

Use request URL and public error code to distinguish cases.

- [ ] **Step 4: Define route metadata**

Routes:

```text
/login      publicOnly
/register   publicOnly
/dashboard  requiresAuth
/profile    requiresAuth
```

Redirect `/` to `/dashboard`.

- [ ] **Step 5: Add global guard**

The guard must call `restoreSession()` exactly once before making redirect decisions.

- [ ] **Step 6: Initialize auth before mount when required**

Update `main.ts` so Pinia exists before store restoration. Avoid a blank infinite state: application mount must complete after authentication initialization resolves or fails safely.

- [ ] **Step 7: Run tests and type check**

```powershell
Set-Location stockmentor-frontend
& npm.cmd run test:unit
& npm.cmd run type-check
```

- [ ] **Step 8: Commit**

```powershell
Set-Location ..
git add stockmentor-frontend/src/api/http.ts `
        stockmentor-frontend/src/router `
        stockmentor-frontend/src/main.ts
git commit -m "feat: add auth interceptors and route guards"
```

---

### Task 15: Build Login, Registration, Dashboard Placeholder and Profile Views

**Files:**
- Create: `stockmentor-frontend/src/features/auth/views/LoginView.vue`
- Create: `stockmentor-frontend/src/features/auth/views/RegisterView.vue`
- Create: `stockmentor-frontend/src/features/auth/views/AuthDashboardView.vue`
- Create: `stockmentor-frontend/src/features/profile/views/ProfileView.vue`
- Modify: `stockmentor-frontend/src/App.vue`
- Modify: `stockmentor-frontend/src/router/index.ts`

**Interfaces:**
- Produces: complete V0.2 browser flow without V0.6 dashboard features

- [ ] **Step 1: Implement login view**

Form fields:

```text
email
password
```

Behavior:

- validate non-empty and length;
- call `authStore.login`;
- show server-safe error;
- disable submit while pending;
- navigate to `/dashboard` on success;
- link to `/register`;
- display investment-education-only notice.

Do not store password after submit.

- [ ] **Step 2: Implement registration view**

Fields:

```text
email
nickname
password
confirmPassword
```

Rules:

- password confirmation is frontend-only;
- do not send `confirmPassword`;
- show 8–64, letter and number rule;
- call `authStore.register`;
- navigate to `/dashboard` after automatic login;
- handle 409 email conflict.

- [ ] **Step 3: Implement authenticated dashboard placeholder**

Display only:

- `欢迎，<nickname>`
- email
- role
- `V0.2 用户与认证已完成`
- `课程系统将在 V0.3 开放`
- profile link
- logout button

Do not add learning progress charts, portfolio performance or course data.

- [ ] **Step 4: Implement profile view**

Display:

- email read-only;
- nickname input;
- save button;
- pending, success and error feedback;
- update store after save;
- logout button.

- [ ] **Step 5: Update App root**

Use `<RouterView />` and a minimal shell. Authentication pages must not be wrapped in a trading-terminal visual style.

- [ ] **Step 6: Run frontend tests and build**

```powershell
Set-Location stockmentor-frontend
& npm.cmd run test:unit
& npm.cmd run type-check
& npm.cmd run build
```

Expected: all pass. Existing chunk-size warning may remain documented as non-blocking; do not add ECharts to authentication pages.

- [ ] **Step 7: Commit**

```powershell
Set-Location ..
git add stockmentor-frontend/src/features `
        stockmentor-frontend/src/App.vue `
        stockmentor-frontend/src/router/index.ts
git commit -m "feat: add authentication user interface"
```

---

### Task 16: Verify the Full Browser Authentication Flow

**Files:**
- Evidence only in ignored runtime report
- Modify code only when a reproduced defect has a proven root cause

**Interfaces:**
- Consumes: real backend and frontend
- Produces: manual end-to-end evidence

- [ ] **Step 1: Start MySQL and backend safely**

Use the Task 11 environment and a fresh database or a deliberately reset dedicated test database. Do not alter unrelated databases.

- [ ] **Step 2: Start frontend**

Use an explicit API configuration or Vite proxy that reaches the backend. Do not commit machine-specific URLs.

- [ ] **Step 3: Verify registration**

In browser:

1. open `/register`;
2. submit mixed-case email with surrounding spaces;
3. verify automatic redirect to `/dashboard`;
4. verify page displays normalized email and nickname;
5. verify `sessionStorage` contains only the two approved keys.

- [ ] **Step 4: Verify refresh and logout**

1. refresh `/dashboard`;
2. verify login is restored;
3. click logout;
4. verify both session keys are removed;
5. verify protected route redirects to `/login`.

- [ ] **Step 5: Verify login errors**

- wrong password shows `邮箱或密码错误`;
- nonexistent email shows the same public message;
- no route loop occurs;
- password field is not persisted.

- [ ] **Step 6: Verify nickname update**

1. login;
2. open `/profile`;
3. update nickname;
4. verify dashboard shows new nickname;
5. verify the same Token remains usable.

- [ ] **Step 7: Verify server-side invalidation**

Disable the test user in the database.
Refresh a protected page using the old Token.

Expected:

- backend returns 401;
- frontend clears session;
- frontend redirects to login;
- no repeated redirects or request loop.

- [ ] **Step 8: Clean up**

Stop only processes started for this test.
Verify no residual listeners on development/test ports.
Keep the installed MySQL service state unchanged.

---

### Task 17: Complete Documentation, Full Verification and Stage Report

**Files:**
- Modify: `README.md`
- Modify: `docs/learning-notes/jwt-authentication.md`
- Modify: `docs/learning-notes/spring-security-flow.md`
- Modify: `docs/learning-notes/database-design.md`
- Modify: `docs/learning-notes/transaction-design.md`
- Modify: `docs/learning-notes/common-bugs.md`
- Modify: `docs/learning-notes/interview-questions.md`
- Modify: `docs/06-development/phase-checklists/v0.2-authentication.md`
- Create: `docs/changelog/2026-07-xx-v0.2-authentication.md`

**Interfaces:**
- Produces: independently reviewable V0.2 delivery evidence

- [ ] **Step 1: Run the final backend suite**

```powershell
$env:JAVA_HOME = 'C:\Users\11707\AppData\Local\Temp\stockmentor-temurin17\expanded\jdk-17.0.19+10'
& mvn.cmd -f stockmentor-backend\pom.xml clean test
& mvn.cmd -f stockmentor-backend\pom.xml clean package
```

Record exact exit codes, test count, failures, errors, skipped count and JAR size.

- [ ] **Step 2: Run the final frontend suite**

```powershell
Set-Location stockmentor-frontend
& npm.cmd ci
& npm.cmd run test:unit
& npm.cmd run type-check
& npm.cmd run build
Set-Location ..
```

Record exact exit codes and test counts.

- [ ] **Step 3: Repeat runtime smoke verification**

Verify:

```text
GET  /api/v1/system/health
GET  /v3/api-docs
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/users/me
PATCH /api/v1/users/me/nickname
```

Record HTTP status and safe response summaries.

- [ ] **Step 4: Scan for secrets and forbidden fields**

Search tracked files for:

```text
JWT_SECRET=
AI_API_KEY=
DB_PASSWORD=
Bearer ey
password_hash in response DTOs
localStorage
```

Example files may contain variable names and replacement text, but no usable secret.

- [ ] **Step 5: Verify logs**

Confirm:

- no generated default Spring password;
- no plaintext submitted password;
- no full JWT;
- no JWT secret;
- no database password.

- [ ] **Step 6: Update README**

Document:

- V0.2 scope;
- JWT environment requirement;
- register/login/current user/nickname endpoints;
- frontend authentication startup;
- verified commands;
- V0.3 not yet implemented.

- [ ] **Step 7: Update learning notes with real paths**

Each note must reference actual classes and tests created in V0.2. Do not describe future behavior as implemented.

- [ ] **Step 8: Complete checklist**

Mark only actually verified items.

- [ ] **Step 9: Write changelog**

The changelog must begin with `# StockMentor V0.2 用户与认证`. Immediately below it, write five lines in this order: the real Asia/Shanghai completion timestamp, `时区：Asia/Shanghai`, `分支：codex/v0.2-authentication`, the full SHA returned by `git rev-parse main`, and the full SHA returned by `git rev-parse HEAD`. Do not use template markers or shortened invented values.

Then include:

- added/modified files;
- registration and login behavior;
- JWT/Security flow;
- Flyway V2 results;
- frontend behavior;
- exact tests;
- runtime evidence;
- secret scan;
- known non-blocking warnings;
- incomplete items;
- next phase V0.3.

Replace every angle-bracket field with actual evidence before commit.

- [ ] **Step 10: Inspect final diff**

```powershell
git diff --check
git status
git log --oneline --decorate -20
git diff main...HEAD --stat
```

Ensure no V0.3 or later feature is present.

- [ ] **Step 11: Commit final documentation**

```powershell
git add README.md docs
git commit -m "docs: complete v0.2 authentication verification"
```

- [ ] **Step 12: Run final clean-tree check**

```powershell
git status
```

Expected: working tree clean.

---

## V0.2 Acceptance Criteria

V0.2 may be presented for PR review only when all items below have fresh evidence:

1. Registration returns 201 and automatically authenticates.
2. Email is trimmed, lowercased and unique.
3. Password is stored only as BCrypt.
4. Wrong password and nonexistent email expose the same public failure.
5. JWT contains only `sub`, `iat`, `exp`, `jti`.
6. Expired, altered and wrong-key Tokens are rejected.
7. Disabled or deleted users cannot use previously issued Tokens.
8. `/api/v1/users/me` returns the current user without internal fields.
9. Nickname update is restricted to the authenticated user.
10. Registration cannot set role or status.
11. Frontend stores authentication only in `sessionStorage`.
12. Refresh restores a valid session.
13. 401 clears authentication and redirects once.
14. Login failure does not create a redirect loop.
15. Logout is local and does not pretend to revoke the Token server-side.
16. Flyway V2 applies after V1 on MySQL 8.
17. Backend full tests and package pass under Java 17.
18. Frontend unit tests, type-check and build pass.
19. Health and OpenAPI remain public and operational.
20. No default password, plaintext password, full JWT or secret appears in logs or Git.
21. README, learning notes, checklist and changelog contain actual evidence.
22. No V0.3 or later business module is implemented.

## Implementation Handoff

Recommended execution mode:

```text
Subagent-Driven
```

Codex should dispatch a fresh worker for each task, then run:

1. specification compliance review;
2. code quality and security review;
3. task-specific tests;
4. commit only after both reviews pass.

The branch remains `codex/v0.2-authentication` until PR approval. Do not merge automatically.
