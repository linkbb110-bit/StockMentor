# API 规范

## 路径

统一前缀：

```text
/api/v1
```

示例：

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `GET /api/v1/courses`
- `POST /api/v1/quizzes/{quizId}/attempts`

## 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {}
}
```

## 分页响应 data

```json
{
  "records": [],
  "page": 1,
  "size": 10,
  "total": 100,
  "pages": 10
}
```

## 错误响应

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "data": null
}
```

## 认证

```text
Authorization: Bearer <access-token>
```

## 约定

- JSON 字段使用 camelCase。
- 数据库字段使用 snake_case。
- 日期时间返回 ISO 8601 格式。
- 金额作为 JSON 数值传输，不在后端使用 `double`。
- 创建资源返回 HTTP 201。
- 参数错误不返回 HTTP 200。
