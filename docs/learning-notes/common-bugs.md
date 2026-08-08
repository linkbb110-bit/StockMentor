# 常见错误记录

## V0.2 环境与验证

- 最终验证必须显式使用 Java 17；本机默认 Java 21 不能作为阶段证据。
- 当前 PowerShell 的 PATH 不一定包含 `mvn.cmd`，应先确认实际 Maven 绝对路径；网络受限导致 POM 解析失败时，先证明是 socket/沙箱问题，不能归因于测试失败。
- PowerShell 执行策略可能拦截 `npm.ps1`，使用 `npm.cmd`。
- 临时 MySQL 必须使用独立端口和数据目录，并核验 PID 所有权、监听器、数据目录清理及已安装 `MySQL84` 状态不变。

## 后端安全错误

- `LoginRequest.java` 和 `RegisterRequest.java` 若沿用 record 默认 `toString()`，密码可能进入 MVC DEBUG 日志；V0.2 显式输出 `password=REDACTED`。
- BCrypt 的边界是 UTF-8 字节而不是 Java 字符数。只校验 8–64 字符会让多字节密码超过 72 字节，并可能在编码时抛错或在匹配时发生前缀截断；DTO、Service 和前端必须复用同一条“不超过 72 UTF-8 字节”规则，且不得 trim、预哈希或改写密码。
- `AuthenticationService.java` 必须让错误密码和不存在邮箱都返回 `AUTH_INVALID_CREDENTIALS`，避免账号枚举。
- `JwtTokenProvider.java` 不能只解码 Token；必须校验签名、过期时间和 `sub`。
- `JwtAuthenticationFilter.java` 不能信任旧状态；每次请求必须经 `SecurityUserService.java` 读取数据库。
- `SecurityBaselineConfig.java` 的 CORS 来源不能使用通配符，且不能回退到 Form Login、HTTP Basic 或随机默认密码。

对应回归测试是 `AuthControllerTest.java`、`AuthenticationServiceTest.java`、`JwtTokenProviderTest.java`、`JwtAuthenticationFilterTest.java` 和 `SecurityConfigTest.java`。

## 前端认证错误

- `authSession.ts` 不能保存完整服务响应或密码，只保存 Token 和安全用户资料；`authSession.spec.ts` 还明确断言不访问 `localStorage`。
- `http.ts` 的登录失败 401 不能触发全局重定向，否则会形成登录循环；受保护请求 401 只能在请求发出时捕获的 Token + generation 仍属当前会话时清理，并发合并也必须按该归属隔离。由 `http.spec.ts` 覆盖。
- `authStore.ts` 恢复会话时必须向 `/users/me` 验证 Token；登录、注册、恢复、资料和昵称等异步结果只能由仍持有相同 Token + generation 的操作落盘，旧结果不得复活或覆盖新会话。由 `authStore.spec.ts` 和视图测试覆盖。
- 构建 exit 0 仍可能有性能告警；2026-08-08 的 Vite 构建报告主 JS chunk 超过 500 kB，并提示 `vite:vue` 插件耗时，均属于已记录的非阻断优化项。

## 排查原则

1. 复现并保存完整安全错误。
2. 区分环境、验证工具和产品根因。
3. 产品缺陷先添加聚焦失败测试，再做最小修复。
4. 不删除或跳过失败测试。
5. 重跑受影响测试和阶段全量门禁。
6. 清理运行资源并扫描日志和 tracked 文件中的敏感值。
