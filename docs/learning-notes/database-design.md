# 数据库设计学习笔记

## V0.2 已实现模型

V0.2 只实现用户与认证数据，没有提前创建课程、题库或组合表。迁移 `stockmentor-backend/src/main/resources/db/migration/V2__create_user_and_auth_tables.sql` 在 V1 之后创建 `sys_user`：

- `email VARCHAR(254)` 保存 trim 后的小写邮箱，并由 `uk_sys_user_email` 保证全局唯一。
- `password_hash VARCHAR(100)` 只保存 BCrypt 字符串。
- `role` 和 `status` 分别映射 `UserRole.java`、`UserStatus.java`。
- `last_login_at` 记录注册自动登录或普通登录成功时间。
- `deleted` 用于逻辑删除；身份查询必须排除已删除记录。

数据库实体是 `user/entity/UserEntity.java`，对外响应是 `user/vo/CurrentUserResponse.java` 和 `auth/vo/AuthResponse.java`；响应对象不含 `passwordHash`、`status` 或 `deleted`。

## 持久化路径和隔离

```text
Controller
→ AuthenticationService / UserService
→ UserRepository
→ MyBatisUserRepository
→ UserMapper
→ MySQL
```

`MyBatisUserRepository.java` 的 `findIdentityById`、昵称更新和最后登录时间更新都使用精确用户 ID 并排除逻辑删除记录。邮箱重复先由 Service 查询，数据库唯一索引再处理并发竞态。

## 实际测试证据

- `MyBatisUserRepositoryTest.java` 验证邮箱等值查询、精确用户 ID、`deleted = 0`、昵称更新和最后登录时间更新条件。
- `AuthenticationServiceTest.java` 验证标准化邮箱、BCrypt、默认角色/状态和重复键冲突映射。
- `UserServiceTest.java` 与 `CurrentUserControllerTest.java` 验证只按当前认证用户 ID 查询/更新并返回安全 VO。
- 2026-08-08 的隔离 MySQL 8.4.7 smoke 从空库依次应用 V1/V2，并验证唯一索引与 BCrypt 数据库布尔条件。

## 常见错误与面试追问

- 错误：修改已执行的 Flyway 文件；把 Entity 直接作为 Controller 响应；私有查询遗漏用户 ID；仅依赖“先查询”处理并发唯一性。
- 追问：唯一索引和 Service 预检为何都需要？逻辑删除会怎样影响唯一性？为什么 DTO/VO 与 Entity 必须分离？
