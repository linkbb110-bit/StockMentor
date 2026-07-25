# 环境变量

## 数据库

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

## JWT

- `JWT_SECRET`
- `JWT_EXPIRATION`

## Redis

- `REDIS_ENABLED`
- `REDIS_HOST`
- `REDIS_PORT`

## AI

- `AI_PROVIDER`
- `AI_API_KEY`

## 原则

- 真实值只存在本地环境、部署平台或安全配置中。
- Git 只提交 `.env.example` 和 `application-local.yml.example`。
- 示例文件不得包含可用的生产密钥。
