# StockMentor

StockMentor 是一个面向投资初学者的股票与投资学习系统，同时作为 Java 后端实习项目使用。

## 核心定位

系统帮助用户完成以下学习闭环：

1. 学习股票、基金、债券、指数、财务报表、估值、行业分析和风险管理。
2. 通过题库检验理解程度。
3. 将错误题目沉淀到错题本。
4. 使用 AI 学习导师解释概念、生成复习题并总结薄弱知识点。
5. 使用原创虚拟公司案例训练公司分析能力。
6. 使用人民币 100,000 元虚拟资金记录模拟投资决策并复盘。

系统不提供具体买入或卖出建议，不预测短期涨跌，不承诺收益，不连接券商，也不执行真实交易。

## 当前阶段

V0.1 正在实施中。当前文档包已完成：

- 项目章程与产品范围
- V0.1 至 V1.0 路线图
- 总体架构设计
- 核心数据库设计
- API 基础规范
- 安全设计
- 测试策略
- 文档和 Git 工作规范
- V0.1 工程基线实施计划
- Codex 首次执行提示词

当前尚未创建业务代码。Codex 必须先检查实际环境，再按照 V0.1 计划逐项实施。

环境检查发现以下缺口：

- Java 17 未安装或未加入 PATH（当前为 Java 21）。
- Maven 和 MySQL 未加入 PATH，虽可通过本机安装路径调用。
- PowerShell 执行策略阻止 `npm.ps1`，可通过 `npm.cmd` 调用 npm。
- Docker 未安装或未加入 PATH。

## 入口文档

- 总体设计：`docs/superpowers/specs/2026-07-25-stockmentor-design.md`
- V0.1 实施计划：`docs/superpowers/plans/2026-07-25-stockmentor-v0.1-foundation.md`
- Codex 首次提示词：`docs/06-development/codex-v0.1-prompt.md`
- 完成定义：`docs/06-development/definition-of-done.md`
- 路线图：`docs/00-project/roadmap.md`

## 预定目录

```text
StockMentor/
├── AGENTS.md
├── README.md
├── docs/
├── stockmentor-backend/
└── stockmentor-frontend/
```

V0.1 完成前，不得提前开发课程、题库、AI、公司分析或虚拟投资业务。
