# 数据库核心实体设计

## 用户

```text
sys_user
- id
- email
- password_hash
- nickname
- role
- status
- last_login_at
- created_at
- updated_at
- deleted
```

## 课程

```text
course 1 ---- n chapter
chapter 1 ---- n lesson
sys_user 1 ---- n learning_progress
lesson 1 ---- n learning_progress
```

主要表：

- `course`
- `chapter`
- `lesson`
- `learning_progress`
- `learning_record`

## 题库

```text
quiz n ---- n question
question 1 ---- n question_option
sys_user 1 ---- n quiz_attempt
quiz_attempt 1 ---- n quiz_answer
sys_user 1 ---- n wrong_question
question 1 ---- n wrong_question
```

主要表：

- `question`
- `question_option`
- `quiz`
- `quiz_question`
- `quiz_attempt`
- `quiz_answer`
- `wrong_question`

## AI 导师

- `tutor_conversation`
- `tutor_message`
- `knowledge_weakness`
- `review_question`

## 公司分析

```text
company_case 1 ---- n company_analysis
company_analysis 1 ---- n analysis_section
company_analysis 1 ---- n analysis_feedback
```

## 虚拟组合

```text
sys_user 1 ---- n virtual_portfolio
virtual_portfolio 1 ---- n virtual_position
virtual_portfolio 1 ---- n virtual_transaction
virtual_transaction 1 ---- n investment_journal
```

## 通用字段

主要业务表统一使用：

- `id BIGINT`
- `created_at DATETIME`
- `updated_at DATETIME`
- `deleted TINYINT`

用户私有表必须包含 `user_id BIGINT`。

金额字段统一使用 `DECIMAL(19,4)`，Java 统一使用 `BigDecimal`。
