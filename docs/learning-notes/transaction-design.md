# 事务设计学习笔记

## V0.2 已实现事务边界

V0.2 只记录认证相关事务，不把后续课程、测验或虚拟交易描述成已实现。

`stockmentor-backend/src/main/java/com/stockmentor/auth/service/AuthenticationService.java` 的 `register` 使用 `@Transactional`，边界包含：邮箱重复预检、BCrypt 哈希、用户插入和首次 `last_login_at` 更新。只有数据库步骤全部完成后才调用 `JwtTokenProvider.issue`；首次登录时间更新失败会抛异常，事务回滚用户写入且不签发 Token。

普通 `login` 不包住密码验证的长事务，只执行一次最后登录时间更新；更新失败不会签发 Token。`stockmentor-backend/src/main/java/com/stockmentor/user/service/UserService.java` 的 `updateNickname` 使用短事务，按当前用户 ID 更新后再读取最新安全资料。

## 为什么事务放在 Service

Controller 只处理协议和校验，Repository 只做持久化。Service 同时知道业务完成条件和失败语义，因此适合定义原子边界。异常不能被吞掉，否则 Spring 代理看不到失败并可能提交部分结果；JWT 这种外部可见结果也不能早于数据库提交条件产生。

## 实际测试证据

- `AuthenticationServiceTest.java` 的 `registerDeclaresTransactionalBoundary` 和 `registerDoesNotIssueTokenWhenFirstLoginUpdateFails` 验证注册边界与签发顺序。
- 同一测试类的 `loginAuthenticatesActiveUserThenUpdatesLastLoginBeforeIssuingToken` 和 `loginDoesNotIssueTokenWhenLastLoginUpdateFails` 验证登录顺序。
- `UserServiceTest.java` 的 `updateNicknameDeclaresTheRequiredShortTransactionBoundary` 验证昵称短事务。
- `MyBatisUserRepositoryTest.java` 验证更新语句只命中指定的非删除用户。

## 常见错误与面试追问

- 错误：同类自调用绕过事务代理；捕获异常后不抛出；在事务完成前签发 Token；把慢外部调用放进事务；更新条件缺少用户 ID。
- 追问：为什么注册需要事务而密码验证不需要长事务？默认哪些异常触发回滚？如何让唯一索引竞态得到稳定业务错误？
