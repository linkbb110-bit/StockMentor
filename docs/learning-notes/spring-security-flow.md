# Spring Security 请求流程学习笔记

## 1. 这个模块解决什么问题

Spring Security 在请求进入 Controller 前完成认证、授权和异常处理，防止未登录用户或无权限用户访问受保护接口。

## 2. 为什么这样设计

安全逻辑集中在过滤链中，比在每个 Controller 手动判断 Token 更一致、更容易测试，也能减少遗漏。

## 3. 请求经过哪些类

```text
Servlet Container
→ SecurityFilterChain
→ CORS Filter
→ JwtAuthenticationFilter
→ AnonymousAuthenticationFilter
→ AuthorizationFilter
→ Controller
```

JWT 有效时，过滤器创建 `Authentication` 并写入 `SecurityContextHolder`。授权阶段再根据路径和角色决定是否允许访问。

## 4. 使用的 Java 和 Spring 知识

- `SecurityFilterChain`
- `HttpSecurity`
- `OncePerRequestFilter`
- `AuthenticationEntryPoint`
- `AccessDeniedHandler`
- 无状态会话策略
- CSRF 与 CORS
- 方法级权限

## 5. 常见错误

- 把 401 和 403 混在一起。
- 放行 Swagger 却漏掉相关静态资源路径。
- CORS 预检请求被安全过滤器拦截。
- JWT 过滤器注册两次。
- 在异常处理器中返回 HTTP 200。
- 前后端分离 API 仍保留不必要的表单登录。

## 6. 面试官可能追问

- Spring Security 为什么基于过滤器而不是拦截器？
- CSRF 为什么在无状态 Bearer Token API 中通常可以关闭？
- CORS 与 Spring Security 的执行顺序为什么重要？
- 如何实现角色和资源所有权两种授权？
