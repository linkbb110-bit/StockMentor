# 安全设计

## 登录

用户提交邮箱和密码后：

1. 校验请求参数。
2. 按标准化邮箱查询用户。
3. 使用 BCrypt 验证密码。
4. 校验用户状态。
5. 生成有效期两小时的 JWT Access Token。
6. 返回令牌和最小用户信息。

登录失败统一返回“邮箱或密码错误”，不暴露邮箱是否存在。

## 受保护请求

1. 前端发送 `Authorization: Bearer <token>`。
2. JWT 过滤器解析请求头。
3. 校验签名、过期时间和必要声明。
4. 加载用户身份。
5. 写入 `SecurityContext`。
6. 业务层通过统一用户上下文获取当前用户 ID。

## 用户数据隔离

用户私有资源必须在数据库查询条件中同时限制资源 ID 和当前用户 ID。

正确：

```sql
SELECT *
FROM learning_progress
WHERE id = ?
  AND user_id = ?
  AND deleted = 0;
```

不允许先按资源 ID 查询，再只在 Java 中判断归属。

## 密钥与配置

- `JWT_SECRET` 通过环境变量提供。
- 本地示例文件只能放示例值。
- `.env`、真实 `application-local.yml` 和 IDE 私密配置不得提交。
- AI API Key 只允许从环境变量读取。
