# StockMentor

StockMentor 是一个面向投资初学者的股票与投资学习系统，同时作为 Java 后端实习项目使用。V0.1 工程基线已于 2026-07-26 完成并通过最终验证。

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
- Maven `3.9.11`
  - 路径：`C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd`
- Node.js `24.18.0`
- npm `11.16.0`
  - PowerShell 执行策略会阻止 `npm.ps1`，请使用 `npm.cmd` 或 `cmd.exe /d /c`。
- MySQL Community Server `8.4.7`
  - 路径：`C:\Program Files\MySQL\MySQL Server 8.4`
- Docker：未安装或不在 `PATH`，V0.1 不要求 Docker；最终验证直接使用隔离的 MySQL 8.4.7 实例完成。

主要受管依赖为 Spring Boot `3.5.16`、MyBatis-Plus `3.5.17`、Flyway `11.7.2`、MySQL Connector/J `9.7.0` 和 springdoc-openapi `2.8.17`。前端锁定的核心版本包括 Vue `3.5.40`、Vue Router `5.2.0`、Pinia `4.0.2`、Axios `1.18.1`、Element Plus `2.14.3`、ECharts `6.1.0`、Vite `8.1.5`、TypeScript `6.0.3` 与 vue-tsc `3.3.8`。

## 本地数据库准备

以下 SQL 仅用于创建本地开发数据库和最小权限账户。请替换占位密码，不要把真实密码提交到 Git：

```sql
CREATE DATABASE stockmentor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'stockmentor_app'@'127.0.0.1'
  IDENTIFIED BY '<请替换为本地强密码>';

GRANT ALL PRIVILEGES ON stockmentor.*
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
$env:REDIS_ENABLED = 'false'
$env:AI_PROVIDER = 'mock'

& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd' `
  '-Dmaven.repo.local=C:\path\to\maven-cache' `
  -f stockmentor-backend\pom.xml clean package

& "$env:JAVA_HOME\bin\java.exe" `
  -jar stockmentor-backend\target\stockmentor-backend-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=local
```

Redis 和真实 AI API Key 不是 V0.1 启动依赖。不要在命令历史、日志或 tracked 配置中保存真实密码、JWT 密钥或 API Key。

## 启动前端

```powershell
Set-Location stockmentor-frontend
& npm.cmd ci
& npm.cmd run dev
```

前端开发服务器默认由 Vite 提供。可通过本地环境变量设置 API 基础地址，但不要提交真实本地 `.env`。

## V0.1 已实现范围

- Spring Boot 3 / Java 17 / Maven 后端工程基线。
- 统一 `ApiResponse<T>`、稳定错误码和全局异常处理。
- Spring Security 公共端点基线。
- `/api/v1/system/health` 健康检查。
- `/v3/api-docs` OpenAPI 文档与固定元数据。
- Flyway V1 非业务元数据迁移及安全本地配置示例。
- Vue 3 + TypeScript + Vite 前端基线，含 Router、Pinia、Axios、Element Plus 与 ECharts 依赖。
- 环境变量模板、忽略规则、测试和构建门禁。

V0.2 的认证设计与实现尚未开始；用户、课程、题库、错题、AI 导师、仪表盘、公司分析、虚拟组合及其他业务模块均未实现。

## 最终验证

2026-07-26 使用上述工具链实际完成：

```text
mvn clean test       -> BUILD SUCCESS，16/16 测试通过
mvn clean package    -> BUILD SUCCESS，生成可执行 JAR
npm run type-check   -> exit 0
npm run build        -> exit 0
GET /api/v1/system/health -> HTTP 200
GET /v3/api-docs           -> HTTP 200
```

Flyway 已在全新、隔离的 MySQL 8.4.7 空数据库上应用 V1；验证后临时实例和数据目录已清理，已安装的 `MySQL84` 服务保持运行。

## 文档入口

- [总体设计](docs/superpowers/specs/2026-07-25-stockmentor-design.md)
- [V0.1 实施计划](docs/superpowers/plans/2026-07-25-stockmentor-v0.1-foundation.md)
- [Definition of Done](docs/06-development/definition-of-done.md)
- [V0.1 检查表](docs/06-development/phase-checklists/v0.1-foundation.md)
- [V0.1 最终变更记录](docs/changelog/2026-07-25-v0.1-foundation.md)
