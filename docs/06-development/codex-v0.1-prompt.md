# Codex 首次执行提示词：V0.1 工程基线

你正在 StockMentor 仓库中工作。当前只执行 V0.1 工程基线，不得提前开发用户、课程、题库、AI、公司分析、虚拟投资或仪表盘业务。

## 开始前必须阅读

1. `AGENTS.md`
2. `README.md`
3. `docs/superpowers/specs/2026-07-25-stockmentor-design.md`
4. `docs/superpowers/plans/2026-07-25-stockmentor-v0.1-foundation.md`
5. `docs/06-development/coding-standards.md`
6. `docs/06-development/definition-of-done.md`

## 执行要求

1. 检查当前目录、Git 状态和开发环境。
2. 记录 Java、Maven、Node、npm、MySQL 和 Docker 的实际版本或缺失情况。
3. 严格按照 V0.1 实施计划逐任务执行。
4. 使用测试驱动方式实现可测试的基础组件。
5. 每个任务完成并验证后创建范围清晰的 Git commit。
6. 不得提交真实密码、密钥或本地 `.env`。
7. 不得声称未实际运行的测试或构建已经成功。
8. 遇到错误时先描述根因和证据，再进行最小修改。
9. 每个阶段汇报顶部使用 Asia/Shanghai 时间，格式 `YYYY-MM-DD HH:mm:ss`。

## V0.1 目标

创建：

- `stockmentor-backend`
- `stockmentor-frontend`
- MySQL/Flyway 基础配置
- 统一响应结构
- 全局异常处理
- 参数校验示例
- OpenAPI
- 健康检查
- `.env.example`
- `application-local.yml.example`
- 基础后端测试
- 基础前端类型检查与构建
- V0.1 changelog

## 完成前必须实际执行

```bash
mvn clean test
mvn clean package
npm run type-check
npm run build
```

若某个命令因环境缺失无法执行，必须明确写出缺失依赖、实际错误和未验证范围，不得把阶段标记为完全完成。
