# StockMentor V0.1 Engineering Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建可编译、可测试、可构建的 StockMentor 前后端基础工程，并建立数据库迁移、统一响应、异常处理、OpenAPI、环境配置和验证基线。

**Architecture:** 后端为 Java 17 + Spring Boot 3 的模块化单体基础工程，前端为 Vue 3 + TypeScript + Vite。V0.1 只建立工程基础设施，不实现用户、课程、题库、AI、公司分析、虚拟投资或仪表盘业务。

**Tech Stack:** Java 17, Spring Boot 3, Maven, Spring Web, Spring Validation, MyBatis-Plus, MySQL 8, Flyway, OpenAPI, JUnit 5, Vue 3, TypeScript, Vite, Pinia, Vue Router, Axios, Element Plus, ECharts.

## Global Constraints

- Java 必须使用 17。
- 后端必须使用 Spring Boot 3 和 Maven。
- 前端必须使用 Vue 3、TypeScript 和 Vite。
- 架构必须保持前后端分离和模块化单体。
- V0.1 不得实现任何业务模块。
- 不得提交真实密码、JWT 密钥或 API Key。
- 数据库初始化使用 Flyway。
- DTO、VO 和数据库实体必须分离。
- Controller 不得直接调用 Mapper。
- 未实际运行的测试和构建不得声明通过。
- 每个任务完成后创建范围清晰的 Git commit。
- 阶段汇报时间使用 Asia/Shanghai，格式 `YYYY-MM-DD HH:mm:ss`。

---

## Planned File Map

### Repository Root

- `pom.xml` 不在根目录创建，后端保持独立 Maven 工程。
- `.gitignore`：忽略环境文件、IDE 文件、构建产物和前端依赖。
- `.env.example`：记录安全的示例环境变量。
- `README.md`：更新实际运行方式和验证状态。

### Backend

- `stockmentor-backend/pom.xml`：后端依赖和构建配置。
- `stockmentor-backend/src/main/java/com/stockmentor/StockMentorApplication.java`：应用入口。
- `stockmentor-backend/src/main/java/com/stockmentor/common/api/ApiResponse.java`：统一响应。
- `stockmentor-backend/src/main/java/com/stockmentor/common/exception/ErrorCode.java`：稳定错误码。
- `stockmentor-backend/src/main/java/com/stockmentor/common/exception/BusinessException.java`：业务异常。
- `stockmentor-backend/src/main/java/com/stockmentor/common/exception/GlobalExceptionHandler.java`：异常映射。
- `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/OpenApiConfig.java`：OpenAPI 配置。
- `stockmentor-backend/src/main/java/com/stockmentor/system/controller/HealthController.java`：基础健康接口。
- `stockmentor-backend/src/main/resources/application.yml`：公共配置。
- `stockmentor-backend/src/main/resources/application-local.yml.example`：本地配置示例。
- `stockmentor-backend/src/main/resources/db/migration/V1__create_schema_history_baseline.sql`：首个可执行迁移。
- `stockmentor-backend/src/test/java/com/stockmentor/common/api/ApiResponseTest.java`：响应测试。
- `stockmentor-backend/src/test/java/com/stockmentor/system/controller/HealthControllerTest.java`：Web 层测试。
- `stockmentor-backend/src/test/java/com/stockmentor/StockMentorApplicationTests.java`：上下文测试。

### Frontend

- `stockmentor-frontend/package.json`：脚本和依赖。
- `stockmentor-frontend/vite.config.ts`：Vite 配置。
- `stockmentor-frontend/tsconfig.json`：TypeScript 配置。
- `stockmentor-frontend/src/main.ts`：应用入口。
- `stockmentor-frontend/src/App.vue`：根组件。
- `stockmentor-frontend/src/router/index.ts`：路由基础。
- `stockmentor-frontend/src/stores/index.ts`：Pinia 基础。
- `stockmentor-frontend/src/api/http.ts`：Axios 实例。
- `stockmentor-frontend/src/types/api.ts`：统一响应类型。
- `stockmentor-frontend/src/views/FoundationView.vue`：V0.1 基础页面。

---

### Task 1: Inspect Repository and Toolchain

**Files:**
- Create: `docs/changelog/2026-07-25-v0.1-environment-check.md`
- Modify: none
- Test: shell version commands

**Interfaces:**
- Consumes: current repository and local toolchain
- Produces: documented facts used by every later task

- [ ] **Step 1: Inspect directory and Git state**

Run:

```powershell
Get-Location
Get-ChildItem -Force
git status
git branch --show-current
git log -5 --oneline
```

Expected: record the real path, files, branch, dirty state and recent commits. If the directory is not a Git repository, record that fact and initialize only after confirming the directory is the StockMentor project root.

- [ ] **Step 2: Inspect required tool versions**

Run:

```powershell
git --version
java -version
mvn -version
node --version
npm --version
mysql --version
docker --version
```

Expected: record each real version or the exact command-not-found error.

- [ ] **Step 3: Write environment check changelog**

Create `docs/changelog/2026-07-25-v0.1-environment-check.md` with the heading `# V0.1 环境检查` and these fields in this exact order:

1. 完成时间
2. 项目路径
3. Git 状态
4. Git 版本
5. Java 版本
6. Maven 版本
7. Node.js 版本
8. npm 版本
9. MySQL 版本
10. Docker 版本
11. 阻塞问题

Every field must contain the actual command output or a concise factual summary of that output. When a command is unavailable, paste the exact command-not-found message. When there is no blocker, write `无`. Do not use placeholder markers or invented versions.

- [ ] **Step 4: Commit the environment record**

```bash
git add docs/changelog/2026-07-25-v0.1-environment-check.md
git commit -m "docs: record v0.1 development environment"
```

Expected: one documentation-only commit.

---

### Task 2: Establish Repository Safety Files

**Files:**
- Create: `.gitignore`
- Create: `.env.example`
- Modify: `README.md`
- Test: `git status --ignored`

**Interfaces:**
- Consumes: environment facts from Task 1
- Produces: repository-level secret and build-output protection

- [ ] **Step 1: Create `.gitignore`**

Use exactly these categories:

```gitignore
# Environment and secrets
.env
.env.*
!.env.example
**/application-local.yml
**/application-prod.yml

# Java and Maven
**/target/
*.class
*.log

# Node and Vite
**/node_modules/
**/dist/
**/.vite/

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# OS
.DS_Store
Thumbs.db

# Test and coverage
coverage/
*.exec
```

- [ ] **Step 2: Create `.env.example`**

```dotenv
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=stockmentor
DB_USERNAME=stockmentor_app
DB_PASSWORD=replace-with-local-password

JWT_SECRET=replace-with-at-least-32-random-characters
JWT_EXPIRATION=7200

REDIS_ENABLED=false
REDIS_HOST=127.0.0.1
REDIS_PORT=6379

AI_PROVIDER=mock
AI_API_KEY=
```

These are examples only. Do not put a usable production secret in this file.

- [ ] **Step 3: Update README current-stage section**

Document that V0.1 is under implementation and list the exact environment gaps found in Task 1.

- [ ] **Step 4: Verify ignore rules**

Run:

```powershell
Copy-Item .env.example .env
git status --ignored
Remove-Item .env
```

Expected: `.env` appears as ignored and `.env.example` remains trackable.

- [ ] **Step 5: Commit repository safety files**

```bash
git add .gitignore .env.example README.md
git commit -m "chore: add repository safety configuration"
```

---

### Task 3: Scaffold the Spring Boot Backend

**Files:**
- Create: `stockmentor-backend/pom.xml`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/StockMentorApplication.java`
- Create: `stockmentor-backend/src/main/resources/application.yml`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/StockMentorApplicationTests.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/StockMentorApplicationTests.java`

**Interfaces:**
- Consumes: Java 17 and Maven
- Produces: `com.stockmentor.StockMentorApplication` and a bootable Spring context

- [ ] **Step 1: Create the Maven project descriptor**

Use a current Spring Boot 3 release available in the environment and record the exact version in the changelog. Include:

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `mybatis-plus-spring-boot3-starter`
- MySQL Connector/J
- Flyway Core
- Flyway MySQL support when required by the selected Flyway version
- OpenAPI starter compatible with Spring Boot 3
- `spring-boot-starter-test`
- Mockito through the test starter

Set:

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

- [ ] **Step 2: Write a failing application context test**

```java
package com.stockmentor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockMentorApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 3: Run the test and capture the initial failure**

Run:

```powershell
cd stockmentor-backend
mvn -Dtest=StockMentorApplicationTests test
```

Expected before application class/configuration exists: FAIL with missing application configuration or compilation error.

- [ ] **Step 4: Create the application entry point**

```java
package com.stockmentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockMentorApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockMentorApplication.class, args);
    }
}
```

- [ ] **Step 5: Create safe common configuration**

`application.yml` must read database settings from environment variables and keep Redis and AI optional. For the context test, use a test profile or test properties that do not require a developer's MySQL instance merely to compile common components. Do not silently claim MySQL integration is tested until an actual database-backed test runs.

- [ ] **Step 6: Run the application context test**

```powershell
mvn -Dtest=StockMentorApplicationTests test
```

Expected: PASS, or a clearly documented database configuration failure that is fixed through a dedicated test profile rather than by deleting Flyway/MyBatis dependencies.

- [ ] **Step 7: Commit backend scaffold**

```bash
git add stockmentor-backend
git commit -m "chore: scaffold spring boot backend"
```

---

### Task 4: Implement Unified API Response

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/common/api/ApiResponse.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/common/api/ApiResponseTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/common/api/ApiResponseTest.java`

**Interfaces:**
- Consumes: none
- Produces: `ApiResponse<T>`, `ApiResponse.success(T)`, `ApiResponse.failure(String, String)`

- [ ] **Step 1: Write failing response tests**

Test these exact behaviors:

```java
package com.stockmentor.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successShouldContainSuccessCodeAndData() {
        ApiResponse<String> response = ApiResponse.success("ready");

        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("操作成功");
        assertThat(response.data()).isEqualTo("ready");
    }

    @Test
    void failureShouldContainProvidedErrorAndNullData() {
        ApiResponse<Void> response =
            ApiResponse.failure("VALIDATION_FAILED", "请求参数不合法");

        assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.message()).isEqualTo("请求参数不合法");
        assertThat(response.data()).isNull();
    }
}
```

- [ ] **Step 2: Run the test to verify failure**

```powershell
mvn -Dtest=ApiResponseTest test
```

Expected: FAIL because `ApiResponse` does not exist.

- [ ] **Step 3: Implement the minimal immutable response**

```java
package com.stockmentor.common.api;

public record ApiResponse<T>(
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 4: Run the response tests**

```powershell
mvn -Dtest=ApiResponseTest test
```

Expected: PASS.

- [ ] **Step 5: Commit unified response**

```bash
git add stockmentor-backend/src/main/java/com/stockmentor/common/api/ApiResponse.java stockmentor-backend/src/test/java/com/stockmentor/common/api/ApiResponseTest.java
git commit -m "feat: add unified api response"
```

---

### Task 5: Implement Error Codes and Global Exception Handling

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/common/exception/ErrorCode.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/common/exception/BusinessException.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/common/exception/GlobalExceptionHandler.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/common/exception/GlobalExceptionHandlerTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/common/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `ApiResponse.failure(String, String)`
- Produces: stable error codes and HTTP mappings for business and validation errors

- [ ] **Step 1: Define the initial error code contract in a failing test**

Verify:

- `VALIDATION_FAILED` maps to 400.
- `RESOURCE_NOT_FOUND` maps to 404.
- `CONFLICT` maps to 409.
- `INTERNAL_ERROR` maps to 500.
- `BusinessException` preserves the selected error code.

- [ ] **Step 2: Run the test and verify failure**

```powershell
mvn -Dtest=GlobalExceptionHandlerTest test
```

Expected: FAIL because exception classes do not exist.

- [ ] **Step 3: Implement `ErrorCode`**

Use an enum with:

```java
VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "请求参数不合法"),
RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
CONFLICT(HttpStatus.CONFLICT, "资源冲突"),
INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
```

Expose `httpStatus()` and `message()`.

- [ ] **Step 4: Implement `BusinessException`**

It must accept an `ErrorCode` and optionally a safe user-facing message. It must not accept raw stack traces or database messages as client messages.

- [ ] **Step 5: Implement `GlobalExceptionHandler`**

Handle at least:

- `BusinessException`
- `MethodArgumentNotValidException`
- `ConstraintViolationException`
- unreadable JSON request
- generic `Exception`

For generic exceptions, log the full exception server-side and return `INTERNAL_ERROR` without internal details.

- [ ] **Step 6: Run exception tests**

```powershell
mvn -Dtest=GlobalExceptionHandlerTest test
```

Expected: PASS.

- [ ] **Step 7: Commit exception foundation**

```bash
git add stockmentor-backend/src/main/java/com/stockmentor/common/exception stockmentor-backend/src/test/java/com/stockmentor/common/exception
git commit -m "feat: add global exception handling"
```

---

### Task 6: Add Health Endpoint and Web-Layer Verification

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/system/controller/HealthController.java`
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java`
- Create: `stockmentor-backend/src/test/java/com/stockmentor/system/controller/HealthControllerTest.java`
- Test: `stockmentor-backend/src/test/java/com/stockmentor/system/controller/HealthControllerTest.java`

**Interfaces:**
- Consumes: `ApiResponse.success(T)`
- Produces: `GET /api/v1/system/health`

- [ ] **Step 1: Write the failing MockMvc test**

Verify HTTP 200 and this JSON shape:

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "status": "UP",
    "service": "stockmentor-backend"
  }
}
```

- [ ] **Step 2: Run the test and verify failure**

```powershell
mvn -Dtest=HealthControllerTest test
```

Expected: FAIL with 404 or missing controller.

- [ ] **Step 3: Implement the health controller**

Create an immutable health payload and return it through `ApiResponse.success`.

- [ ] **Step 4: Add baseline Security configuration**

Permit:

- `/api/v1/system/health`
- OpenAPI JSON
- Swagger UI assets

Require authentication for all other routes, even though V0.1 has no protected business endpoint yet. Disable CSRF only for the stateless JSON API baseline and document that browser form authentication is not used.

- [ ] **Step 5: Run the health test**

```powershell
mvn -Dtest=HealthControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit health baseline**

```bash
git add stockmentor-backend/src/main/java/com/stockmentor/system stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java stockmentor-backend/src/test/java/com/stockmentor/system
git commit -m "feat: add public health endpoint"
```

---

### Task 7: Configure OpenAPI

**Files:**
- Create: `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/OpenApiConfig.java`
- Modify: `stockmentor-backend/src/main/resources/application.yml`
- Test: application startup and OpenAPI endpoint

**Interfaces:**
- Consumes: Spring Boot web application
- Produces: OpenAPI document and Swagger UI

- [ ] **Step 1: Configure API metadata**

Use:

- Title: `StockMentor API`
- Version: `v1`
- Description: `股票与投资学习系统 API，仅用于投资教育和虚拟学习。`

- [ ] **Step 2: Start the backend**

```powershell
mvn spring-boot:run
```

Expected: application starts without requiring Redis or an AI API Key.

- [ ] **Step 3: Verify OpenAPI and health**

In another shell:

```powershell
curl.exe -i http://localhost:8080/api/v1/system/health
curl.exe -i http://localhost:8080/v3/api-docs
```

Expected: both return HTTP 200. Record the actual Swagger UI path provided by the selected library version.

- [ ] **Step 4: Stop the backend and commit**

```bash
git add stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/OpenApiConfig.java stockmentor-backend/src/main/resources/application.yml
git commit -m "feat: configure openapi documentation"
```

---

### Task 8: Establish Flyway Baseline and Local Configuration Example

**Files:**
- Create: `stockmentor-backend/src/main/resources/db/migration/V1__create_schema_history_baseline.sql`
- Create: `stockmentor-backend/src/main/resources/application-local.yml.example`
- Modify: `stockmentor-backend/src/main/resources/application.yml`
- Test: Flyway migration against a real local MySQL 8 database

**Interfaces:**
- Consumes: environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- Produces: repeatable application startup from an empty MySQL database

- [ ] **Step 1: Create the first migration**

The V1 migration must create one non-business table to prove migrations execute:

```sql
CREATE TABLE system_metadata (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_metadata_key (metadata_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

Insert:

```sql
INSERT INTO system_metadata (metadata_key, metadata_value)
VALUES ('schema_version', 'v0.1');
```

- [ ] **Step 2: Create `application-local.yml.example`**

It must show how to activate local configuration while reading all credentials from environment variables. It must not include a real password.

- [ ] **Step 3: Create or select an empty local MySQL database**

Use a dedicated local database named `stockmentor`. Do not drop unrelated databases. Record the exact SQL or administration steps used.

- [ ] **Step 4: Run the backend with the local profile**

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Expected: Flyway creates its history table and `system_metadata`.

- [ ] **Step 5: Verify migration data**

Run a MySQL query equivalent to:

```sql
SELECT metadata_key, metadata_value
FROM system_metadata
WHERE metadata_key = 'schema_version';
```

Expected: one row with `v0.1`.

- [ ] **Step 6: Commit database baseline**

```bash
git add stockmentor-backend/src/main/resources
git commit -m "feat: add flyway database baseline"
```

---

### Task 9: Scaffold the Vue Frontend

**Files:**
- Create: `stockmentor-frontend/package.json`
- Create: `stockmentor-frontend/vite.config.ts`
- Create: `stockmentor-frontend/tsconfig.json`
- Create: `stockmentor-frontend/tsconfig.app.json`
- Create: `stockmentor-frontend/index.html`
- Create: `stockmentor-frontend/src/main.ts`
- Create: `stockmentor-frontend/src/App.vue`
- Create: `stockmentor-frontend/src/router/index.ts`
- Create: `stockmentor-frontend/src/stores/index.ts`
- Create: `stockmentor-frontend/src/api/http.ts`
- Create: `stockmentor-frontend/src/types/api.ts`
- Create: `stockmentor-frontend/src/views/FoundationView.vue`
- Test: TypeScript check and Vite build

**Interfaces:**
- Consumes: Node.js and npm
- Produces: Vue application with router, Pinia, Axios, Element Plus and ECharts dependencies

- [ ] **Step 1: Create the Vite Vue TypeScript project**

Use the official Vite Vue TypeScript scaffold in `stockmentor-frontend`. Do not overwrite repository documentation.

- [ ] **Step 2: Install required dependencies**

Install production dependencies:

```text
vue
vue-router
pinia
axios
element-plus
echarts
```

Retain the scaffold's supported TypeScript and Vite versions.

- [ ] **Step 3: Define unified API response type**

```ts
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}
```

- [ ] **Step 4: Create Axios baseline**

`src/api/http.ts` must:

- read base URL from `VITE_API_BASE_URL`
- default to `/api/v1`
- set JSON headers
- set a reasonable timeout
- reject errors without hiding the original Axios error
- not implement JWT behavior before V0.2

- [ ] **Step 5: Create foundation page**

Display:

- product name
- education-only description
- current version `V0.1`
- backend health endpoint path
- clear text that business modules are not yet implemented

- [ ] **Step 6: Add scripts**

Ensure `package.json` provides:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "type-check": "vue-tsc -b",
    "preview": "vite preview"
  }
}
```

Adapt only if the scaffold's generated TypeScript project references require the equivalent supported command.

- [ ] **Step 7: Run type check**

```powershell
npm run type-check
```

Expected: PASS.

- [ ] **Step 8: Run production build**

```powershell
npm run build
```

Expected: PASS and `dist` created.

- [ ] **Step 9: Commit frontend scaffold**

```bash
git add stockmentor-frontend
git commit -m "chore: scaffold vue frontend"
```

---

### Task 10: Verify Full V0.1 Build and Update Documentation

**Files:**
- Modify: `README.md`
- Create: `docs/changelog/2026-07-25-v0.1-foundation.md`
- Modify: `docs/06-development/phase-checklists/v0.1-foundation.md`
- Test: all V0.1 verification commands

**Interfaces:**
- Consumes: all previous tasks
- Produces: independently verifiable V0.1 engineering baseline

- [ ] **Step 1: Run all backend tests**

```powershell
cd stockmentor-backend
mvn clean test
```

Expected: PASS. Record test count and failures.

- [ ] **Step 2: Build the backend package**

```powershell
mvn clean package
```

Expected: PASS and executable JAR created under `target`.

- [ ] **Step 3: Run all frontend checks**

```powershell
cd ..\stockmentor-frontend
npm run type-check
npm run build
```

Expected: both PASS.

- [ ] **Step 4: Verify backend runtime endpoints**

Start the backend with actual local environment configuration, then run:

```powershell
curl.exe -i http://localhost:8080/api/v1/system/health
curl.exe -i http://localhost:8080/v3/api-docs
```

Expected: HTTP 200.

- [ ] **Step 5: Scan tracked files for obvious secrets**

Run repository-appropriate searches for:

```text
password=
JWT_SECRET=
AI_API_KEY=
sk-
Bearer ey
```

Review matches manually. Example files may contain variable names and replacement text, but no usable secret.

- [ ] **Step 6: Update README**

Document:

- actual prerequisites and versions
- backend startup command
- frontend startup command
- MySQL database setup
- environment variable usage
- verified commands and dates
- V0.1 implemented scope
- V0.2 not yet implemented scope

- [ ] **Step 7: Complete V0.1 checklist**

Mark only items actually verified. Leave failed or blocked items unchecked and explain them in the changelog.

- [ ] **Step 8: Write V0.1 changelog**

Include:

- actual Asia/Shanghai completion time
- files added and modified
- implemented capabilities
- exact commands
- actual results
- errors and root causes
- unresolved items
- next phase: V0.2 authentication design and plan

- [ ] **Step 9: Commit V0.1 completion**

```bash
git add README.md docs/06-development/phase-checklists/v0.1-foundation.md docs/changelog/2026-07-25-v0.1-foundation.md
git commit -m "docs: complete v0.1 foundation verification"
```

- [ ] **Step 10: Inspect final repository state**

```powershell
git status
git log --oneline --decorate -12
```

Expected: clean working tree and a sequence of focused commits. If the tree is not clean, explain every remaining file before claiming completion.

---

## V0.1 Acceptance Criteria

V0.1 is accepted only when:

1. Backend tests pass.
2. Backend package builds.
3. Frontend type check passes.
4. Frontend production build passes.
5. Health and OpenAPI endpoints return HTTP 200.
6. Flyway executes against an empty MySQL 8 database.
7. Redis and AI API Key are not required for startup.
8. `.env` is ignored and `.env.example` is tracked.
9. No business module is implemented early.
10. README and changelog contain actual verification evidence.
