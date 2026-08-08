# JWT 认证学习笔记

## V0.2 解决的问题

V0.2 使用两小时有效的 JWT Access Token 证明请求身份，不实现 Refresh Token、黑名单或后端退出接口。实现入口是 `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/security/JwtTokenProvider.java`，配置由同目录的 `JwtProperties.java` 读取 `JWT_SECRET` 和 `JWT_EXPIRATION`；密钥缺失或少于 32 字节时启动失败。

## 签发与认证流程

注册或登录：

```text
AuthController
→ AuthenticationService
→ PasswordEncoder / UserRepository
→ JwtTokenProvider.issue
→ AuthResponse
```

受保护请求：

```text
Authorization: Bearer <token>
→ JwtAuthenticationFilter
→ JwtTokenProvider.parseUserId
→ SecurityUserService
→ UserRepository.findIdentityById
→ SecurityContextHolder
→ Controller
```

`JwtTokenProvider` 只签发 `sub`、`iat`、`exp`、`jti` 四个声明，不把邮箱、昵称、角色或状态放入 Token。`JwtAuthenticationFilter.java` 每次请求都通过 `SecurityUserService.java` 重新读取数据库身份，因此用户禁用或逻辑删除后，旧 Token 会立即得到 401。

## 前端会话

`stockmentor-frontend/src/features/auth/session/authSession.ts` 只使用 `sessionStorage` 的 `stockmentor.accessToken` 和 `stockmentor.currentUser`。`stockmentor-frontend/src/api/http.ts` 注入 Bearer Token；本地退出会清除这两个键。Token 被窃取仍可在到期前被使用，因此 HTTPS、日志脱敏和较短有效期仍是必要边界。

## 实际测试证据

- `JwtTokenProviderTest.java` 验证 HS256、最小声明、唯一 `jti`、过期、错误签名、篡改和非法 `sub`。
- `JwtAuthenticationFilterTest.java` 验证数据库身份重载、禁用/删除用户、401 映射和完整 Token 不进入日志。
- `JwtPropertiesTest.java` 验证密钥和过期配置边界。
- `authSession.spec.ts` 验证只保存允许字段、不使用 `localStorage`；`http.spec.ts` 验证 Token 注入和 401 清理。

## 常见错误与面试追问

- 错误：硬编码或打印 JWT 密钥、完整 Token；只解码不校验签名和过期；长期相信 Token 中的角色。
- 追问：JWT 与 Session 的取舍是什么？401 与 403 如何区分？无黑名单时“退出”代表什么？为什么 V0.2 仍保留 `jti`？
