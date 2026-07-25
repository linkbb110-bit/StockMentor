# AI 学习导师设计笔记

## 1. 这个模块解决什么问题

AI 导师负责解释概念、结合课程上下文答疑、解释错题、生成复习题和总结薄弱知识点，同时保持投资教育边界。

## 2. 为什么这样设计

业务层依赖 `AiTutorService` 接口，而不是依赖具体模型 SDK。第一阶段使用 Mock 实现，确保项目无需 API Key 也能运行和测试。后续接入 Spring AI 时只增加适配器。

## 3. 请求经过哪些类

```text
TutorController
→ TutorApplicationService
→ SafetyPolicy
→ AiTutorService
    ├── MockAiTutorService
    └── SpringAiTutorService
→ TutorConversationRepository
→ TutorChatResponse
```

## 4. 使用的 Java 和 Spring 知识

- 接口与依赖倒置
- 策略选择
- `@ConditionalOnProperty`
- 配置属性
- Prompt 模板
- 超时和降级
- 可测试性
- 内容安全规则

## 5. 安全边界

AI 不得：

- 提供具体买入卖出建议。
- 预测短期涨跌。
- 承诺收益。
- 提供目标价或最佳买点。
- 鼓励杠杆、融资、期权或做空。

遇到具体投资决策问题时，转为商业模式、财务、估值、竞争和风险分析框架，并提示独立研究。

## 6. 常见错误

- Controller 直接调用模型 SDK。
- API Key 写入配置文件并提交 Git。
- Mock 与真实实现返回结构不同。
- 将模型输出直接当作可靠事实。
- 没有超时、长度和错误处理。
- 只在前端写免责声明，服务端没有安全规则。

## 7. 面试官可能追问

- 为什么需要 Mock AI？
- 如何通过配置切换实现？
- 如何测试非确定性模型输出？
- 如何做 Prompt Injection 防护？
- 模型不可用时系统如何降级？
