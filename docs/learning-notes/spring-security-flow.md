# Spring Security 请求流程学习笔记

## V0.2 实际过滤链

配置位于 `stockmentor-backend/src/main/java/com/stockmentor/infrastructure/config/SecurityBaselineConfig.java`。文件名沿用 V0.1 基线，但 V0.2 已加入 BCrypt、显式 CORS、无状态会话、JWT Filter 和统一 401/403：

```text
Servlet Container
→ CORS
→ JwtAuthenticationFilter
→ AnonymousAuthenticationFilter
→ AuthorizationFilter
→ Controller
```

`POST /api/v1/auth/register`、`POST /api/v1/auth/login`、健康检查和 OpenAPI 公开；其余 `/api/v1/**` 默认需要认证。Form Login、HTTP Basic、CSRF 和服务端 Session 均未作为认证机制使用，策略为 `SessionCreationPolicy.STATELESS`。

## 身份和异常职责

- `JwtAuthenticationFilter.java` 读取并验证 Bearer Token，只在身份有效时写入 `SecurityContextHolder`。
- `SecurityUserService.java` 从数据库读取当前状态和最新角色，返回 `AuthenticatedUser.java`。
- `RestAuthenticationEntryPoint.java` 直接输出统一 JSON 401。
- `RestAccessDeniedHandler.java` 直接输出统一 JSON 403。
- `CorsProperties.java` 从 `CORS_ALLOWED_ORIGINS` 读取显式来源；拒绝空值、空白值和通配符。

认证失败表示身份缺失或无效，对应 401；身份有效但权限不足才是 403。过滤器处理可识别的认证失败后不继续链路，避免重复响应。

## 实际测试证据

- `SecurityConfigTest.java` 验证公开/受保护路径、统一 401/403、允许来源预检和非白名单来源拒绝。
- `JwtAuthenticationFilterTest.java` 验证 Bearer 格式、SecurityContext 写入/清理和每请求身份重载。
- `SecurityUserServiceTest.java` 验证有效、禁用和不存在用户的内部分类。
- 前端 `stockmentor-frontend/src/api/http.spec.ts` 验证登录 401 不触发循环，而受保护请求 401 只清理并跳转一次。

## 常见错误与面试追问

- 错误：把 401/403 混用；重复注册 JWT Filter；使用 `*` CORS；让预检被认证拦截；保留默认表单登录或随机密码日志。
- 追问：为什么 Security 基于过滤器？CORS 与认证的先后关系是什么？Bearer Token API 为什么可以关闭 CSRF？资源所有权授权应放在哪一层？
