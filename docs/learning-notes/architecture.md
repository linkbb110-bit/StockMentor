# 架构学习笔记

## 解决什么问题

说明 StockMentor 为什么使用模块化单体，以及如何在单体中保持业务边界。

## 为什么这样设计

微服务会引入服务发现、网关、远程调用、分布式事务和部署复杂度。当前项目更需要完整业务闭环和可讲解的代码结构，因此采用模块化单体。

## 请求经过哪些类

典型请求：

```text
Controller
→ Application Service
→ Domain Rule
→ Repository / Mapper
→ MySQL
```

认证请求还会先经过 Spring Security 过滤链。

## Java 和 Spring 知识

- 依赖注入
- 接口与实现分离
- 分层架构
- 事务边界
- 配置管理
- 模块内聚和低耦合

## 常见错误

- 只按 Controller、Service、Mapper 横向分包，业务边界不清。
- Controller 直接调用 Mapper。
- 一个 Service 类承担多个模块职责。
- 为了显得高级而过早使用微服务。

## 面试追问

- 模块化单体与普通单体有什么区别？
- 什么时候应该拆微服务？
- 如何阻止模块之间随意调用？
- Dashboard 跨模块查询应该怎样设计？
