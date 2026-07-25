# ADR-003：AI 第一阶段使用 Mock 实现

## 状态

已批准。

## 决策

先定义 `AiTutorService` 接口并提供 `MockAiTutorService`。真实模型接入作为后续适配器，不影响业务 Controller 和 Service。

## 原因

- 项目无需 API Key 即可完整运行。
- 测试结果稳定。
- 避免网络、配额和模型输出不确定性阻塞核心业务。
- 可以先验证安全边界和接口设计。

## 后果

- Mock 输出必须明确是学习反馈。
- 通过配置 `stockmentor.ai.provider=mock` 选择实现。
- 真实实现必须读取环境变量中的 API Key。
