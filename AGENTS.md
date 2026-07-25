# StockMentor Agent Instructions

本文件适用于在 StockMentor 仓库中工作的 Codex 或其他编码代理。

## 一、执行原则

1. 先阅读：
   - `README.md`
   - `docs/superpowers/specs/2026-07-25-stockmentor-design.md`
   - 当前阶段对应的实施计划
   - `docs/06-development/coding-standards.md`
   - `docs/06-development/definition-of-done.md`
2. 未经用户明确批准，不得扩展当前阶段范围。
3. 当前任务只实施计划中列出的文件和行为。
4. 遇到错误时先定位根因，再进行最小范围修改。
5. 不得以“理论上可行”冒充“已经验证”。
6. 每个任务完成后执行对应测试并提交一次范围清晰的 Git commit。
7. 不提交真实密码、API Key、JWT 密钥或本地 `.env` 文件。
8. 不复制受版权保护教材的大段内容。
9. 所有投资相关功能必须保持教育边界，不得提供具体买卖建议、收益承诺或短期涨跌预测。

## 二、架构约束

- Java 17
- Spring Boot 3
- Maven
- Vue 3 + TypeScript + Vite
- 模块化单体
- 前后端分离
- MySQL 8
- Flyway
- DTO/VO 与数据库实体分离
- 统一异常处理
- 用户数据按当前登录用户 ID 隔离
- 金额使用 `BigDecimal`
- 数据库金额字段使用 `DECIMAL(19,4)`
- 时间使用 `LocalDateTime`
- 密码使用 BCrypt
- JWT 密钥从环境变量读取

## 三、范围控制

V1.0 不包含：

- 实时行情
- 券商连接
- 真实交易
- 荐股
- 短期涨跌预测
- 收益承诺
- 杠杆、融资、期权、做空
- 微服务
- 消息队列
- 完整管理员后台
- 邮箱验证码
- 找回密码
- 第三方登录
- Refresh Token
- 社区、评论、私信
- 文件上传

## 四、阶段汇报格式

每阶段结束后，在 `docs/changelog/` 新建记录，必须包含：

- 完成时间：Asia/Shanghai，`YYYY-MM-DD HH:mm:ss`
- 阶段名称
- 新增文件
- 修改文件
- 实现内容
- 实际执行的命令
- 测试与构建结果
- 遇到的问题与根因
- 尚未完成内容
- 下一阶段建议

## 五、禁止行为

- 不得跳过失败测试。
- 不得删除测试来让构建通过。
- 不得把数据库实体直接作为 Controller 返回值。
- 不得在 Controller 中直接调用 Mapper。
- 不得把密码或完整 JWT 写入日志。
- 不得在没有 `user_id` 条件时查询用户私有资源。
- 不得擅自更换技术栈。
- 不得一次性生成全部系统。
