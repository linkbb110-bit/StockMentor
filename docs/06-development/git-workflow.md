# Git 工作流

## 分支

个人开发阶段使用：

```text
main
└── feature/<phase-or-feature>
```

示例：

```text
feature/v0.1-foundation
feature/auth
feature/course-progress
```

## 提交流程

1. 从最新 `main` 创建功能分支。
2. 按实施计划逐任务开发。
3. 每个独立可验证任务提交一次。
4. 运行阶段要求的测试和构建。
5. 更新 changelog。
6. 审查 diff，确认无密钥和无范围外修改。
7. 合并前保留清晰的提交历史。

## 禁止

- 不在一个提交中混合多个业务模块。
- 不使用“update”“fix stuff”等模糊提交信息。
- 不提交生成目录、IDE 私密配置和真实环境文件。
- 不通过删除测试解决构建失败。
