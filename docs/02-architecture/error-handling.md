# 异常处理设计

## 异常类型

- `BusinessException`：可预期业务规则失败。
- `ResourceNotFoundException`：资源不存在或对当前用户不可见。
- `UnauthorizedException`：未认证或令牌无效。
- `ForbiddenException`：已认证但无权限。
- `ConflictException`：邮箱重复等资源冲突。

## HTTP 映射

| 情况 | HTTP 状态 |
|---|---|
| 参数格式或业务参数错误 | 400 |
| 未认证 | 401 |
| 无权限 | 403 |
| 资源不存在 | 404 |
| 唯一约束冲突 | 409 |
| 未知服务端错误 | 500 |

## 错误响应

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "邮箱或密码错误",
  "data": null
}
```

未知异常只向客户端返回通用信息，服务端日志保留完整堆栈。
