# StockMentor V1.0 系统设计规范

## 1. 状态

- 设计状态：已批准
- 批准日期：2026-07-25
- 架构：前后端分离、模块化单体
- 当前实施阶段：V0.1 工程基线

## 2. 产品目标

StockMentor 面向投资初学者，通过课程、题库、错题、AI 学习导师、公司分析训练和虚拟投资日志，建立以知识、证据、风险和复盘为中心的学习闭环。

项目同时用于 Java 后端实习展示，重点体现 Spring Security、JWT、MyBatis-Plus、数据库设计、事务、测试、聚合统计和 AI 接口抽象能力。

## 3. 安全与产品边界

系统仅用于投资教育：

- 不提供具体个股买入或卖出建议。
- 不预测具体股票短期涨跌。
- 不承诺收益。
- 不连接券商。
- 不执行真实交易。
- 不支持杠杆、融资、期权、做空和高风险衍生品。
- 所有组合和交易均使用虚拟资金。
- 用户询问具体投资决策时，AI 转为讲解分析框架并提示独立研究。
- 禁止使用“稳赚”“必涨”“最佳买点”等语言。

## 4. 技术栈

### 后端

- Java 17
- Spring Boot 3
- Maven
- Spring Web
- Spring Validation
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8
- Redis 可选配置
- Flyway
- OpenAPI 或 Knife4j
- JUnit 5
- Mockito

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios
- Element Plus
- ECharts

### AI

- `AiTutorService`
- `MockAiTutorService`
- 后续预留 Spring AI
- API Key 只从环境变量读取

## 5. 架构

```text
Vue 3
  |
  | HTTP / JSON
  v
Spring Boot
  |
  +-- Security + JWT
  +-- Auth
  +-- User
  +-- Course
  +-- Quiz
  +-- Tutor
  +-- CompanyAnalysis
  +-- Portfolio
  +-- Dashboard
  |
  v
MyBatis-Plus
  |
  v
MySQL 8
```

Redis 和真实 AI Provider 均不得成为 V0.1 至 V0.6 的强制启动依赖。

## 6. 账号体系

- 邮箱和密码注册登录。
- 邮箱全局唯一。
- 昵称可修改且不要求唯一。
- 密码使用 BCrypt。
- JWT Access Token 有效期两小时。
- V1.0 不实现 Refresh Token。
- V1.0 不实现邮箱验证码、找回密码和第三方登录。
- 用户表保留 `USER`、`ADMIN` 角色，但不开发独立管理后台。

## 7. 课程

结构：

```text
Course
└── Chapter
    └── Lesson
```

Lesson 包含标题、摘要、原创 Markdown 正文、预计学习时长、排序和发布状态。

学习进度规则：

- 普通用户只能读取已发布内容。
- 用户只能修改自己的进度。
- `(user_id, lesson_id)` 唯一。
- 重复标记完成保持幂等。
- 下一节课程按课程、章节、课时排序计算。

课程初始主题：

1. 股票、基金、债券和指数
2. 收益与风险
3. 公司和商业模式
4. 利润表
5. 资产负债表
6. 现金流量表
7. 常见估值指标
8. 行业与竞争分析
9. 风险和分散
10. 投资复盘

## 8. 题库

题型：

- 单选
- 多选
- 判断
- 简答

评分：

- 单选和判断要求答案相等。
- 多选比较集合，顺序不影响结果，必须完全匹配。
- 简答题不计入客观题正确率。
- 简答题由 Mock AI 按概念覆盖、逻辑完整和风险意识给出三级反馈：
  - `NEEDS_REVIEW`
  - `BASIC_UNDERSTANDING`
  - `GOOD_UNDERSTANDING`

错题规则：

- 客观题答错时新增或更新用户错题记录。
- 错误次数加一并记录最近错误时间。
- 答对后标记掌握，不物理删除。
- 再次答错时恢复待复习状态。

## 9. AI 导师

接口至少提供：

```java
TutorChatResponse chat(TutorChatRequest request);

List<ReviewQuestion> generateReviewQuestions(
    Long userId,
    List<KnowledgeWeakness> weaknesses
);

List<KnowledgeWeakness> summarizeWeaknesses(Long userId);
```

第一阶段使用 Mock 实现。真实实现通过配置切换，不能改变上层业务接口。

## 10. 公司分析

使用原创虚拟公司案例。模板包含：

1. 公司如何赚钱
2. 核心客户
3. 收入来源
4. 主要成本
5. 行业竞争
6. 竞争优势
7. 财务趋势
8. 风险因素
9. 尚未确认的信息
10. 最终总结

系统只评价完整性、证据、逻辑跳跃、事实与推测区分和风险识别，不给出买卖结论。

## 11. 虚拟投资日志

- 默认初始虚拟资金为人民币 100,000 元。
- 不接实时行情。
- 用户手动输入学习模拟价格。
- 界面明确标注数据不是市场实时数据。
- 数量和价格必须大于零。
- 买入金额不得超过可用虚拟资金。
- 卖出数量不得超过持仓。
- 金额使用 `BigDecimal`。
- 交易使用事务。

移动加权平均成本：

```text
新平均成本 =
（原数量 × 原平均成本 + 新买入数量 × 买入价格）
÷ 新总数量
```

已实现盈亏：

```text
卖出数量 ×（卖出价格 - 当前平均成本）
```

未实现盈亏：

```text
剩余数量 ×（模拟当前价格 - 平均成本）
```

## 12. 仪表盘

展示：

- 已完成课时数和总课时数
- 学习进度
- 客观题正确率
- 待复习错题数量
- 薄弱知识点
- 最近学习记录
- 虚拟组合表现
- 下一步学习建议

建议规则：

1. 有待复习错题时优先复习。
2. 否则推荐下一节未完成课时。
3. 否则根据薄弱知识点推荐复习题。
4. 否则推荐公司分析或投资复盘。

## 13. 数据库

核心表：

- `sys_user`
- `course`
- `chapter`
- `lesson`
- `learning_progress`
- `learning_record`
- `question`
- `question_option`
- `quiz`
- `quiz_question`
- `quiz_attempt`
- `quiz_answer`
- `wrong_question`
- `tutor_conversation`
- `tutor_message`
- `knowledge_weakness`
- `review_question`
- `company_case`
- `company_analysis`
- `analysis_section`
- `analysis_feedback`
- `virtual_portfolio`
- `virtual_position`
- `virtual_transaction`
- `investment_journal`
- `user_activity_log`

主要业务表包含 `id`、`created_at`、`updated_at` 和 `deleted`。用户私有表包含 `user_id`。

金额字段使用 `DECIMAL(19,4)`，Java 使用 `BigDecimal`。

## 14. API

统一前缀：`/api/v1`

成功：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {}
}
```

失败：

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "邮箱或密码错误",
  "data": null
}
```

不得把所有错误包装成 HTTP 200。

## 15. 安全

- BCrypt
- JWT 密钥环境变量
- DTO 参数校验
- 用户数据数据库层隔离
- CORS 白名单
- 日志脱敏
- SQL 参数化
- 生产环境隐藏内部异常
- 登录错误不透露邮箱存在性
- 不提交真实密码和密钥

## 16. 测试

后端阶段至少运行：

```bash
mvn clean test
mvn clean package
```

前端阶段至少运行：

```bash
npm run type-check
npm run build
```

关键测试包括：

- 注册和密码验证
- JWT 生成与解析
- 401 和 403
- 用户 A 无法访问用户 B 数据
- 课程完成幂等
- 多选集合评分
- 错题状态变化
- BigDecimal 平均成本和盈亏
- 交易失败事务回滚

## 17. 版本顺序

- V0.1 工程基线
- V0.2 用户与认证
- V0.3 课程与进度
- V0.4 题库与错题
- V0.5 AI Mock
- V0.6 仪表盘
- V0.7 公司分析
- V0.8 虚拟投资日志
- V0.9 工程完善
- V1.0 简历发布

任何阶段不得提前实现后续业务。
