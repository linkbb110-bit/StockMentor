# 架构总览

## 架构风格

StockMentor 采用前后端分离的模块化单体架构。

```text
Vue 3 + TypeScript
        |
        | HTTP / JSON
        v
Spring Boot REST API
        |
        +-- Spring Security + JWT
        +-- Auth / User / Course / Quiz
        +-- Tutor / CompanyAnalysis
        +-- Portfolio / Dashboard
        |
        v
MyBatis-Plus
        |
        v
MySQL 8
```

Redis 在 V1.0 中是可选基础设施，不得成为应用启动的强制依赖。AI 第一阶段使用 Mock 实现，应用不需要 API Key 即可运行。

## 模块边界

- `auth`：注册、登录、JWT 签发和认证入口。
- `user`：用户资料和状态。
- `course`：课程、章节、课时和进度。
- `quiz`：题目、测验、评分、答题和错题。
- `tutor`：AI 服务接口、会话、复习题和薄弱点。
- `companyanalysis`：公司案例、分析和反馈。
- `portfolio`：虚拟组合、持仓、交易和日志。
- `dashboard`：跨模块只读聚合。
- `common`：通用响应、异常、分页和用户上下文。
- `infrastructure`：配置、持久化、安全、Redis 和 AI Provider 适配。

## 依赖方向

- Controller 依赖 Service。
- Service 依赖领域规则和 Repository 接口。
- Mapper 作为持久化实现存在。
- Dashboard 可以读取其他模块提供的查询接口，不直接修改其他模块数据。
- 业务模块不得依赖前端类型。
