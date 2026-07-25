# 索引设计

## 唯一索引

- `sys_user(email)`
- `learning_progress(user_id, lesson_id)`
- `wrong_question(user_id, question_id)`
- `virtual_position(portfolio_id, stock_code)`

## 普通索引

- `chapter(course_id, sort_order)`
- `lesson(chapter_id, sort_order)`
- `quiz_attempt(user_id, submitted_at)`
- `quiz_answer(attempt_id, question_id)`
- `wrong_question(user_id, mastered, last_wrong_at)`
- `company_analysis(user_id, company_case_id, created_at)`
- `virtual_transaction(portfolio_id, traded_at)`
- `learning_record(user_id, occurred_at)`

## 原则

- 索引应服务于真实查询条件。
- 不对每个字段机械建索引。
- 联合索引字段顺序按等值过滤、范围过滤和排序需求决定。
- 所有索引需在对应版本的查询实现后通过执行计划检查。
