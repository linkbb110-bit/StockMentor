# StockMentor V0.2 面试问题清单

## 架构与职责

1. 为什么 V0.2 继续采用模块化单体，并把 `auth`、`user`、`infrastructure/security` 分开？
2. `AuthController.java`、`AuthenticationService.java`、`UserRepository.java` 各自负责什么，为什么 Controller 不直接调用 Mapper？
3. 为什么 `UserEntity.java` 不能直接作为响应，而要使用 `AuthResponse.java` 和 `CurrentUserResponse.java`？
4. `AuthenticationServiceTest.java` 和 `AuthControllerTest.java` 分别验证哪个层次？

## JWT 与 Spring Security

1. `JwtTokenProvider.java` 为什么只写入 `sub`、`iat`、`exp`、`jti`？`JwtTokenProviderTest.java` 如何证明？
2. `JwtAuthenticationFilter.java` 为什么每次请求都调用 `SecurityUserService.java` 查询数据库？性能与即时失效如何取舍？
3. `RestAuthenticationEntryPoint.java` 和 `RestAccessDeniedHandler.java` 分别处理 401 与 403 的哪种场景？
4. `SecurityBaselineConfig.java` 为什么采用 `STATELESS`，并关闭 Form Login 与 HTTP Basic？
5. `SecurityConfigTest.java` 如何验证公开路径、保护路径和 CORS 白名单？

## 密码、用户与事务

1. BCrypt 为什么自带盐，为什么数据库仍要使用 `VARCHAR(100)` 保存哈希？
2. `EmailNormalizer.java` 和 `NicknameNormalizer.java` 为什么先标准化再校验？对应的 `EmailNormalizerTest.java`、`NicknameNormalizerTest.java` 覆盖哪些边界？
3. 为什么错误密码和不存在邮箱必须返回相同响应？
4. 注册时为何由 Service 预查邮箱、同时保留数据库唯一索引？
5. `AuthenticationService.register` 为什么使用事务，且必须在最后登录时间更新成功后才签发 JWT？
6. `UserService.updateNickname` 如何保证只修改当前认证用户，`UserServiceTest.java` 与 `MyBatisUserRepositoryTest.java` 如何证明？

## 前端认证

1. `authSession.ts` 为什么选 `sessionStorage` 而不是 `localStorage` 或 Cookie Session？
2. `authStore.ts` 如何恢复会话并调用 `/users/me` 验证 Token？
3. `http.ts` 如何避免登录失败重定向循环和并发 401 重复跳转？`http.spec.ts` 覆盖了哪些情形？
4. 前端无状态退出为什么不等于服务端撤销 Token？
5. `routerGuards.spec.ts` 如何验证未登录访问受保护路由和已登录访问公共认证页？

## 当前边界

1. V0.2 为什么不实现 Refresh Token、验证码、找回密码、黑名单或后端退出接口？
2. V0.3 课程与进度尚未实现时，认证占位仪表盘为什么不能展示虚构学习统计？
3. 如何确保后续用户私有资源继续使用当前认证用户 ID 做数据库层隔离？
