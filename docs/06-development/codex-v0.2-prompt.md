# Codex V0.2 用户与认证执行提示词

你正在 `linkbb110-bit/StockMentor` 仓库中实施 V0.2 用户与认证模块。

## 开始前必须完成

1. 切换到最新 `main` 并执行 `git pull --ff-only origin main`。
2. 确认工作树干净。
3. 创建分支 `codex/v0.2-authentication`。
4. 阅读：
   - `AGENTS.md`
   - `README.md`
   - `docs/superpowers/specs/2026-07-25-stockmentor-design.md`
   - `docs/superpowers/specs/2026-07-26-stockmentor-v0.2-authentication-design.md`
   - `docs/superpowers/plans/2026-07-26-stockmentor-v0.2-authentication.md`
   - `docs/06-development/coding-standards.md`
   - `docs/06-development/definition-of-done.md`
5. 使用 `superpowers:subagent-driven-development` 按实施计划逐任务执行。
6. 在修改前重新运行 V0.1 后端测试、前端 type-check 和 build，确认基线仍然通过。

## 严格范围

本阶段只实现：

- Flyway V2 用户表
- 邮箱密码注册
- 注册成功自动登录
- BCrypt
- JWT Access Token
- Spring Security JWT 过滤链
- 当前用户
- 修改昵称
- 登录/注册/认证占位页/个人中心
- Pinia、sessionStorage、Axios 401、路由守卫
- 对应测试、文档和验证

本阶段禁止实现：

- Refresh Token
- 邮箱验证码
- 找回密码
- 头像
- JWT 黑名单
- 后端退出
- 管理员后台
- 课程
- 题库
- AI 导师
- 正式仪表盘
- 公司分析
- 虚拟投资日志

## 安全约束

- JWT 只包含 `sub`、`iat`、`exp`、`jti`。
- 每个受保护请求重新查询数据库用户状态和最新角色。
- JWT 密钥只从环境变量读取，至少 32 UTF-8 字节。
- 不记录密码、完整 JWT、JWT 密钥和数据库密码。
- 密码只保存 BCrypt。
- 登录失败不能泄露邮箱是否存在。
- 注册请求不能传角色和状态。
- 当前用户不能修改邮箱、角色或状态。
- 前端只使用 `sessionStorage`，禁止 `localStorage`。
- 保留 V0.1 默认密码日志和 `BusinessException` 安全修复。

## 执行纪律

- 每项任务先写失败测试并证明失败原因。
- 只实施当前任务所列文件和接口。
- 每个任务经过规范审查和代码质量审查后再提交。
- 不删除测试来让构建通过。
- 不以模拟结果替代真实 MySQL 和真实 HTTP 验证。
- 遇到错误先定位根因，再做最小修复。
- 每项任务提交一次清晰 Git commit。
- 未实际运行的内容不得声称通过。
- 不自动合并 PR。

## 最终必须执行

后端：

```powershell
mvn.cmd -f stockmentor-backend\pom.xml clean test
mvn.cmd -f stockmentor-backend\pom.xml clean package
```

前端：

```powershell
Set-Location stockmentor-frontend
npm.cmd ci
npm.cmd run test:unit
npm.cmd run type-check
npm.cmd run build
```

运行时：

- 在安全隔离的 MySQL 8 临时数据库中执行 Flyway V1 和 V2。
- 验证注册、登录、当前用户、昵称修改。
- 验证禁用用户后旧 Token 立即返回 401。
- 验证健康检查和 OpenAPI。
- 验证运行结束后无残留进程、监听端口或临时数据目录。
- 验证 Git 中无真实密钥。
- 验证日志中无默认密码、明文密码和完整 Token。

## 阶段报告

报告顶部使用：

```text
完成时间：YYYY-MM-DD HH:mm:ss
时区：Asia/Shanghai
```

报告必须列出：

- 新增和修改文件
- 每项功能
- 每条实际命令
- 测试数量和退出码
- MySQL/Flyway/HTTP 验证
- 安全扫描
- 错误与根因
- 非阻断警告
- 尚未完成内容
- 最终提交哈希
- 工作树状态

完成后停止，不要开始 V0.3。等待用户审查和 PR 指令。
