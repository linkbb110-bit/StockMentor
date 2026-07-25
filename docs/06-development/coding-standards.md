# 编码规范

## Java

- 使用 Java 17。
- 包名全小写。
- 类和方法名称表达业务含义。
- Controller 只负责协议转换、校验和调用 Service。
- Service 负责业务规则和事务边界。
- Mapper 不承载业务判断。
- DTO、VO 和 Entity 分离。
- 金额和比例计算使用 `BigDecimal`。
- 使用构造器注入，不使用字段注入。
- 公开方法和复杂规则写必要注释，不写重复代码含义的注释。
- 业务异常使用稳定错误码。

## TypeScript

- 禁止无理由使用 `any`。
- API 请求和响应定义明确类型。
- 页面逻辑优先放在 feature 目录。
- Axios 调用集中到 API 模块。
- Pinia 只保存跨页面状态。
- 表单、加载和错误状态必须显式处理。

## SQL

- 表名和字段使用 snake_case。
- 迁移文件按 Flyway 版本递增。
- 已应用的迁移不得修改。
- 金额使用 `DECIMAL(19,4)`。
- 时间使用 `DATETIME`。
- 用户私有数据表包含 `user_id`。
- 合理设置唯一索引和普通索引。

## Git

提交信息使用：

```text
docs: ...
chore: ...
feat: ...
fix: ...
test: ...
refactor: ...
```

每次提交只包含一个清晰目的。
