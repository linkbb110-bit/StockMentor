# ADR-002：使用 JWT Access Token

## 状态

已批准。

## 决策

V1.0 使用有效期两小时的 JWT Access Token，不实现 Refresh Token。

## 原因

- 足以展示 Spring Security 和无状态认证流程。
- 减少令牌轮换、撤销列表和双令牌存储复杂度。
- 令牌过期后用户重新登录，适合学习系统第一版。

## 后果

- 前端在收到 401 时清理本地状态并跳转登录。
- JWT 密钥必须从环境变量读取。
- 后续如增加长期会话，再单独设计 Refresh Token。
