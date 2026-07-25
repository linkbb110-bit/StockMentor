# JWT 认证学习笔记

## 1. 这个模块解决什么问题

JWT 用于证明请求来自已经登录的用户。前端登录成功后保存 Access Token，后续请求通过 `Authorization: Bearer <token>` 传递身份。

## 2. 为什么这样设计

V1.0 使用有效期两小时的 Access Token，不实现 Refresh Token。这样可以完整展示无状态认证、过滤器和 SecurityContext，同时避免令牌轮换与撤销列表的额外复杂度。

## 3. 请求经过哪些类

登录流程：

```text
AuthController
→ AuthenticationService
→ UserRepository
→ PasswordEncoder
→ JwtTokenProvider
→ LoginResponseVO
```

受保护请求：

```text
HTTP Request
→ JwtAuthenticationFilter
→ JwtTokenProvider
→ UserDetailsService 或用户查询服务
→ SecurityContextHolder
→ Controller
```

## 4. 使用的 Java 和 Spring 知识

- Spring Security Filter Chain
- `OncePerRequestFilter`
- `Authentication`
- `SecurityContextHolder`
- BCrypt
- JWT 签名、声明和过期时间
- 环境变量配置
- 401 与 403

## 5. 常见错误

- 把 JWT 密钥写死在代码中。
- 使用过短密钥。
- 日志打印完整 Token。
- 只解析 Token，不验证签名和过期时间。
- 将用户权限完全相信 Token，却不考虑用户已经被禁用。
- 认证失败后继续执行过滤链并产生重复响应。

## 6. 面试官可能追问

- JWT 与 Session 的区别是什么？
- JWT 被窃取后怎么办？
- 为什么 Access Token 不适合存放敏感信息？
- 401 与 403 分别在什么情况下返回？
- Refresh Token 应该如何设计？
