CREATE TABLE question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(32) NOT NULL,
    stem VARCHAR(1000) NOT NULL,
    explanation VARCHAR(2000) NOT NULL,
    published TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_question_published (published, id),
    CONSTRAINT chk_question_type CHECK (
        type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE question_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(20) NOT NULL,
    content VARCHAR(500) NOT NULL,
    is_correct TINYINT NOT NULL DEFAULT 0,
    published TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_option_question_key (question_id, option_key),
    UNIQUE KEY uk_question_option_question_sort (question_id, sort_order),
    KEY idx_question_option_published_sort (
        question_id,
        published,
        sort_order,
        id
    ),
    CONSTRAINT chk_question_option_correct CHECK (is_correct IN (0, 1)),
    CONSTRAINT chk_question_option_published CHECK (published IN (0, 1)),
    CONSTRAINT fk_question_option_question
        FOREIGN KEY (question_id) REFERENCES question (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quiz (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    published TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quiz_lesson (lesson_id),
    KEY idx_quiz_published (published, id),
    CONSTRAINT chk_quiz_published CHECK (published IN (0, 1)),
    CONSTRAINT fk_quiz_lesson
        FOREIGN KEY (lesson_id) REFERENCES lesson (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quiz_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quiz_question_quiz_question (quiz_id, question_id),
    UNIQUE KEY uk_quiz_question_quiz_sort (quiz_id, sort_order),
    UNIQUE KEY uk_quiz_question_question (question_id),
    CONSTRAINT fk_quiz_question_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_question_question
        FOREIGN KEY (question_id) REFERENCES question (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quiz_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    total_questions SMALLINT UNSIGNED NOT NULL,
    correct_count SMALLINT UNSIGNED NOT NULL,
    score_percent TINYINT UNSIGNED NOT NULL,
    submitted_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_quiz_attempt_user_submitted (user_id, submitted_at, id),
    KEY idx_quiz_attempt_quiz (quiz_id, id),
    CONSTRAINT chk_quiz_attempt_total CHECK (total_questions > 0),
    CONSTRAINT chk_quiz_attempt_correct CHECK (correct_count <= total_questions),
    CONSTRAINT chk_quiz_attempt_score CHECK (score_percent <= 100),
    CONSTRAINT fk_quiz_attempt_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_attempt_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quiz_answer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    is_correct TINYINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quiz_answer_attempt_question (attempt_id, question_id),
    KEY idx_quiz_answer_question (question_id),
    CONSTRAINT chk_quiz_answer_correct CHECK (is_correct IN (0, 1)),
    CONSTRAINT fk_quiz_answer_attempt
        FOREIGN KEY (attempt_id) REFERENCES quiz_attempt (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_answer_question
        FOREIGN KEY (question_id) REFERENCES question (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quiz_answer_option (
    quiz_answer_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    PRIMARY KEY (quiz_answer_id, option_id),
    KEY idx_quiz_answer_option_option (option_id, quiz_answer_id),
    CONSTRAINT fk_quiz_answer_option_answer
        FOREIGN KEY (quiz_answer_id) REFERENCES quiz_answer (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_answer_option_option
        FOREIGN KEY (option_id) REFERENCES question_option (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wrong_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    error_count INT UNSIGNED NOT NULL,
    last_wrong_at DATETIME NOT NULL,
    mastered_at DATETIME NULL,
    last_reviewed_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wrong_question_user_question (user_id, question_id),
    KEY idx_wrong_question_user_pending (user_id, status, last_wrong_at, id),
    KEY idx_wrong_question_user_mastered (user_id, status, mastered_at, id),
    KEY idx_wrong_question_question (question_id),
    CONSTRAINT chk_wrong_question_status CHECK (status IN ('PENDING', 'MASTERED')),
    CONSTRAINT chk_wrong_question_error_count CHECK (error_count > 0),
    CONSTRAINT fk_wrong_question_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_wrong_question_question
        FOREIGN KEY (question_id) REFERENCES question (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO quiz (id, lesson_id, title, summary, published) VALUES
    (1, 1, '股票、基金与债券课后测验', '检验不同资产所代表的基本权利。', 1),
    (2, 2, '指数基础课后测验', '检验指数样本、权重与解读边界。', 1),
    (3, 3, '收益来源课后测验', '检验收益组成与比较口径。', 1),
    (4, 4, '风险认识课后测验', '检验波动之外的风险来源。', 1),
    (5, 5, '价值创造课后测验', '检验公司价值创造链条。', 1),
    (6, 6, '商业模式课后测验', '检验客户、交付与经济结构。', 1),
    (7, 7, '利润表基础课后测验', '检验利润表层次与期间属性。', 1),
    (8, 8, '利润质量课后测验', '检验持续经营与一次性项目。', 1),
    (9, 9, '资产负债表基础课后测验', '检验会计恒等式与资产质量。', 1),
    (10, 10, '偿债风险课后测验', '检验期限、现金与负债结构。', 1),
    (11, 11, '现金流分类课后测验', '检验经营、投资与筹资现金流。', 1),
    (12, 12, '利润与现金课后测验', '检验权责发生制与现金收付差异。', 1),
    (13, 13, '市盈率课后测验', '检验市盈率含义与常见误区。', 1),
    (14, 14, '相对估值边界课后测验', '检验市净率等指标的适用范围。', 1),
    (15, 15, '行业竞争课后测验', '检验行业结构与竞争力量。', 1),
    (16, 16, '竞争优势证据课后测验', '检验优势假设需要的证据。', 1),
    (17, 17, '分散原则课后测验', '检验风险来源与分散边界。', 1),
    (18, 18, '风险承受能力课后测验', '检验资金期限与知识边界。', 1),
    (19, 19, '结构化记录课后测验', '检验事实、假设、证据与风险记录。', 1),
    (20, 20, '投资复盘课后测验', '检验过程评价与结果偏差。', 1);

INSERT INTO question (id, type, stem, explanation, published) VALUES
    (1, 'SINGLE_CHOICE', '股票通常代表持有人享有什么基本权利？', '股票通常代表公司所有权的一部分，其风险和回报与公司经营相关。', 1),
    (2, 'TRUE_FALSE', '基金份额必然等同于某一家公司的直接所有权。', '基金份额通常对应一篮子资产或投资组合，并不必然等同于单一公司的直接所有权。', 1),
    (3, 'MULTIPLE_CHOICE', '理解一个指数时通常需要关注哪些规则？', '样本范围、权重方法和调整规则共同影响指数所描述的内容。', 1),
    (4, 'SINGLE_CHOICE', '某加权指数上涨时，最准确的理解是什么？', '指数反映按既定规则加权后的整体变化，不代表每个成分都上涨。', 1),
    (5, 'SINGLE_CHOICE', '投资结果的常见组成是什么？', '投资结果可能同时来自价格变化、现金分配，并受到费用等成本影响。', 1),
    (6, 'MULTIPLE_CHOICE', '比较两项投资收益前应统一哪些口径？', '期间、现金分配和费用成本都需要纳入一致口径。', 1),
    (7, 'TRUE_FALSE', '投资风险只等于价格波动。', '价格波动只是风险的一种表现，还可能存在流动性、信用和经营等风险。', 1),
    (8, 'MULTIPLE_CHOICE', '除价格波动外，哪些也可能构成投资风险？', '流动性、信用和经营变化都可能影响损失概率与应对能力。', 1),
    (9, 'SINGLE_CHOICE', '分析公司创造价值时，合理的起点是什么？', '应先理解投入如何转化为活动、客户价值和回报。', 1),
    (10, 'TRUE_FALSE', '公司收入增长就能自动证明价值持续增长。', '收入增长还需结合成本、投入和持续性，不能单独证明价值增长。', 1),
    (11, 'MULTIPLE_CHOICE', '完整的商业模式描述通常应包含哪些要素？', '客户、价值主张、交付方式以及收入成本结构共同构成商业模式。', 1),
    (12, 'SINGLE_CHOICE', '订阅模式与单次销售相比通常更强调什么？', '订阅模式通常更强调持续服务、续费与客户留存。', 1),
    (13, 'SINGLE_CHOICE', '利润表的基本阅读顺序是什么？', '利润表通常从收入开始，扣除成本费用后得到利润。', 1),
    (14, 'TRUE_FALSE', '利润表直接列示某一时点企业拥有的全部现金余额。', '利润表反映一段期间的经营成果，并不是期末现金余额清单。', 1),
    (15, 'MULTIPLE_CHOICE', '判断利润质量时哪些信息更有帮助？', '持续经营来源、现金支持和一次性项目识别有助于判断利润质量。', 1),
    (16, 'SINGLE_CHOICE', '出售资产形成的一次性收益应如何看待？', '它可能提高当期利润，但未必能够在未来持续重复。', 1),
    (17, 'TRUE_FALSE', '资产负债表满足“资产等于负债加所有者权益”的基本关系。', '该恒等关系描述企业资源与其资金来源之间的对应。', 1),
    (18, 'MULTIPLE_CHOICE', '分析资产质量时应关注哪些方面？', '变现能力、减值风险和未来经济利益都比单看账面金额更有解释力。', 1),
    (19, 'SINGLE_CHOICE', '观察短期偿债风险时最应结合什么？', '应把到期义务与可用现金、资产变现能力和融资条件结合。', 1),
    (20, 'TRUE_FALSE', '只看总负债比例就足以判断全部偿债风险。', '负债期限、利率、现金和资产流动性同样影响偿债风险。', 1),
    (21, 'SINGLE_CHOICE', '企业用现金购置生产设备通常属于哪类现金流？', '购置长期设备通常归入投资活动现金流。', 1),
    (22, 'MULTIPLE_CHOICE', '分析现金净增加额时还应分别查看哪些组成？', '经营、投资和筹资现金流的组合能够解释现金变化的来源。', 1),
    (23, 'TRUE_FALSE', '确认利润意味着同一期间一定收到等额现金。', '权责发生制下收入费用确认与现金收付时间可能不同。', 1),
    (24, 'SINGLE_CHOICE', '赊销发生时最可能出现什么情况？', '企业可以确认收入，但客户尚未付款时现金不会同步增加。', 1),
    (25, 'SINGLE_CHOICE', '市盈率主要连接了哪两个量？', '市盈率把市场价格与盈利口径联系起来。', 1),
    (26, 'MULTIPLE_CHOICE', '看到较低市盈率时还应检查哪些因素？', '盈利异常、下降趋势和风险差异都可能使低市盈率产生误导。', 1),
    (27, 'TRUE_FALSE', '不同行业公司市净率相同，就可以认为它们估值含义完全相同。', '资产结构和商业模式差异会改变市净率的解释。', 1),
    (28, 'SINGLE_CHOICE', '选择相对估值指标前应先做什么？', '应先确认指标与业务特征和会计口径是否匹配。', 1),
    (29, 'MULTIPLE_CHOICE', '分析行业竞争强度时应观察哪些力量？', '竞争者、客户、供应商和替代方案都会影响行业结构。', 1),
    (30, 'TRUE_FALSE', '行业规模很大就能保证所有参与者获得良好回报。', '行业规模不能替代对竞争强度、成本和议价关系的分析。', 1),
    (31, 'SINGLE_CHOICE', '哪项更可能为竞争优势提供可跟踪证据？', '长期稳定的复购率可以为客户黏性提供一项可验证证据。', 1),
    (32, 'MULTIPLE_CHOICE', '哪些信息单独出现时不足以证明持久竞争优势？', '知名度、管理层表述和短期高利润都需要更多长期证据支持。', 1),
    (33, 'TRUE_FALSE', '分散投资可以消除所有市场风险。', '分散可降低部分特定风险，但不能消除所有共同市场风险。', 1),
    (34, 'MULTIPLE_CHOICE', '判断是否真正分散时应检查哪些差异？', '应关注风险来源、行业驱动和地域暴露是否真正不同。', 1),
    (35, 'SINGLE_CHOICE', '评估风险承受能力时哪项最关键？', '资金用途、使用期限、财务缓冲和心理感受需要综合考虑。', 1),
    (36, 'TRUE_FALSE', '一次获得正收益就足以证明个人风险承受能力很强。', '单次结果不能证明长期承受能力，也不能替代风险理解。', 1),
    (37, 'MULTIPLE_CHOICE', '结构化投资学习记录应区分哪些内容？', '事实、假设、证据和风险分开记录，便于后续检验推理。', 1),
    (38, 'SINGLE_CHOICE', '怎样让一项假设更便于后续检验？', '写明可观察信号和复核日期，才能判断条件是否发生变化。', 1),
    (39, 'TRUE_FALSE', '只要最终结果理想，就能证明当时的分析过程合理。', '结果可能受偶然因素影响，过程仍需按当时信息独立评价。', 1),
    (40, 'MULTIPLE_CHOICE', '复盘分析过程时应分别评价哪些方面？', '信息质量、推理过程、风险控制和结果条件应分开评价。', 1);

INSERT INTO quiz_question (id, quiz_id, question_id, sort_order) VALUES
    (1, 1, 1, 1),
    (2, 1, 2, 2),
    (3, 2, 3, 1),
    (4, 2, 4, 2),
    (5, 3, 5, 1),
    (6, 3, 6, 2),
    (7, 4, 7, 1),
    (8, 4, 8, 2),
    (9, 5, 9, 1),
    (10, 5, 10, 2),
    (11, 6, 11, 1),
    (12, 6, 12, 2),
    (13, 7, 13, 1),
    (14, 7, 14, 2),
    (15, 8, 15, 1),
    (16, 8, 16, 2),
    (17, 9, 17, 1),
    (18, 9, 18, 2),
    (19, 10, 19, 1),
    (20, 10, 20, 2),
    (21, 11, 21, 1),
    (22, 11, 22, 2),
    (23, 12, 23, 1),
    (24, 12, 24, 2),
    (25, 13, 25, 1),
    (26, 13, 26, 2),
    (27, 14, 27, 1),
    (28, 14, 28, 2),
    (29, 15, 29, 1),
    (30, 15, 30, 2),
    (31, 16, 31, 1),
    (32, 16, 32, 2),
    (33, 17, 33, 1),
    (34, 17, 34, 2),
    (35, 18, 35, 1),
    (36, 18, 36, 2),
    (37, 19, 37, 1),
    (38, 19, 38, 2),
    (39, 20, 39, 1),
    (40, 20, 40, 2);

INSERT INTO question_option (
    id,
    question_id,
    option_key,
    content,
    is_correct,
    published,
    sort_order
) VALUES
    (11, 1, 'A', '公司所有权的一部分', 1, 1, 1),
    (12, 1, 'B', '固定利息的无条件承诺', 0, 1, 2),
    (13, 1, 'C', '一篮子资产的管理规则', 0, 1, 3),
    (14, 1, 'D', '指数的计算结果', 0, 1, 4),
    (21, 2, 'TRUE', '正确', 0, 1, 1),
    (22, 2, 'FALSE', '错误', 1, 1, 2),
    (31, 3, 'A', '样本选择范围', 1, 1, 1),
    (32, 3, 'B', '权重计算方法', 1, 1, 2),
    (33, 3, 'C', '定期调整规则', 1, 1, 3),
    (34, 3, 'D', '保证每个成分同涨同跌', 0, 1, 4),
    (41, 4, 'A', '所有成分价格都上涨', 0, 1, 1),
    (42, 4, 'B', '按规则加权后的整体结果上涨', 1, 1, 2),
    (43, 4, 'C', '未来收益已经确定', 0, 1, 3),
    (44, 4, 'D', '样本公司都没有风险', 0, 1, 4),
    (51, 5, 'A', '价格变化、现金分配并扣除相关成本', 1, 1, 1),
    (52, 5, 'B', '只有价格变化', 0, 1, 2),
    (53, 5, 'C', '只有现金分配', 0, 1, 3),
    (54, 5, 'D', '只看投入金额', 0, 1, 4),
    (61, 6, 'A', '比较期间', 1, 1, 1),
    (62, 6, 'B', '现金分配口径', 1, 1, 2),
    (63, 6, 'C', '费用与税费范围', 1, 1, 3),
    (64, 6, 'D', '只比较最高单日涨幅', 0, 1, 4),
    (71, 7, 'TRUE', '正确', 0, 1, 1),
    (72, 7, 'FALSE', '错误', 1, 1, 2),
    (81, 8, 'A', '流动性不足', 1, 1, 1),
    (82, 8, 'B', '信用状况变化', 1, 1, 2),
    (83, 8, 'C', '公司经营恶化', 1, 1, 3),
    (84, 8, 'D', '价格暂时稳定本身', 0, 1, 4),
    (91, 9, 'A', '投入如何转化为活动、客户价值与回报', 1, 1, 1),
    (92, 9, 'B', '只看公司名称', 0, 1, 2),
    (93, 9, 'C', '只看短期股价', 0, 1, 3),
    (94, 9, 'D', '先假定收入必然持续增长', 0, 1, 4),
    (101, 10, 'TRUE', '正确', 0, 1, 1),
    (102, 10, 'FALSE', '错误', 1, 1, 2),
    (111, 11, 'A', '目标客户', 1, 1, 1),
    (112, 11, 'B', '提供的价值', 1, 1, 2),
    (113, 11, 'C', '交付方式', 1, 1, 3),
    (114, 11, 'D', '收入与成本结构', 1, 1, 4),
    (121, 12, 'A', '持续服务、续费与留存', 1, 1, 1),
    (122, 12, 'B', '一次成交后不再服务', 0, 1, 2),
    (123, 12, 'C', '取消所有成本', 0, 1, 3),
    (124, 12, 'D', '保证客户永不流失', 0, 1, 4),
    (131, 13, 'A', '收入→成本费用→利润', 1, 1, 1),
    (132, 13, 'B', '资产→负债→现金', 0, 1, 2),
    (133, 13, 'C', '现金→股价→收入', 0, 1, 3),
    (134, 13, 'D', '负债→收入→存货', 0, 1, 4),
    (141, 14, 'TRUE', '正确', 0, 1, 1),
    (142, 14, 'FALSE', '错误', 1, 1, 2),
    (151, 15, 'A', '利润是否来自持续经营', 1, 1, 1),
    (152, 15, 'B', '是否得到现金流支持', 1, 1, 2),
    (153, 15, 'C', '是否包含一次性项目', 1, 1, 3),
    (154, 15, 'D', '只看利润标题数字', 0, 1, 4),
    (161, 16, 'A', '可能提高当期利润但未必可重复', 1, 1, 1),
    (162, 16, 'B', '必然改善未来主营业务', 0, 1, 2),
    (163, 16, 'C', '等同于经营现金持续增长', 0, 1, 3),
    (164, 16, 'D', '可以直接忽略交易背景', 0, 1, 4),
    (171, 17, 'TRUE', '正确', 1, 1, 1),
    (172, 17, 'FALSE', '错误', 0, 1, 2),
    (181, 18, 'A', '资产变现能力', 1, 1, 1),
    (182, 18, 'B', '潜在减值风险', 1, 1, 2),
    (183, 18, 'C', '未来经济利益能力', 1, 1, 3),
    (184, 18, 'D', '只看资产账面总额', 0, 1, 4),
    (191, 19, 'A', '到期义务、现金与资产流动性', 1, 1, 1),
    (192, 19, 'B', '公司名称长度', 0, 1, 2),
    (193, 19, 'C', '历史最高股价', 0, 1, 3),
    (194, 19, 'D', '单一收入增长率', 0, 1, 4),
    (201, 20, 'TRUE', '正确', 0, 1, 1),
    (202, 20, 'FALSE', '错误', 1, 1, 2),
    (211, 21, 'A', '经营活动', 0, 1, 1),
    (212, 21, 'B', '投资活动', 1, 1, 2),
    (213, 21, 'C', '筹资活动', 0, 1, 3),
    (214, 21, 'D', '不属于现金流', 0, 1, 4),
    (221, 22, 'A', '经营活动现金流', 1, 1, 1),
    (222, 22, 'B', '投资活动现金流', 1, 1, 2),
    (223, 22, 'C', '筹资活动现金流', 1, 1, 3),
    (224, 22, 'D', '只看期末现金一个数字', 0, 1, 4),
    (231, 23, 'TRUE', '正确', 0, 1, 1),
    (232, 23, 'FALSE', '错误', 1, 1, 2),
    (241, 24, 'A', '收入可确认但现金尚未收到', 1, 1, 1),
    (242, 24, 'B', '现金与收入必然同时增加', 0, 1, 2),
    (243, 24, 'C', '不产生任何应收项目', 0, 1, 3),
    (244, 24, 'D', '利润一定为零', 0, 1, 4),
    (251, 25, 'A', '市场价格与盈利', 1, 1, 1),
    (252, 25, 'B', '现金与负债', 0, 1, 2),
    (253, 25, 'C', '收入与存货', 0, 1, 3),
    (254, 25, 'D', '股息与员工数', 0, 1, 4),
    (261, 26, 'A', '盈利是否异常', 1, 1, 1),
    (262, 26, 'B', '盈利是否在下降', 1, 1, 2),
    (263, 26, 'C', '增长、质量与风险差异', 1, 1, 3),
    (264, 26, 'D', '直接认定一定便宜', 0, 1, 4),
    (271, 27, 'TRUE', '正确', 0, 1, 1),
    (272, 27, 'FALSE', '错误', 1, 1, 2),
    (281, 28, 'A', '确认指标与业务及会计口径匹配', 1, 1, 1),
    (282, 28, 'B', '选择数值最低的指标', 0, 1, 2),
    (283, 28, 'C', '忽略行业差异', 0, 1, 3),
    (284, 28, 'D', '假设一个指标适合所有公司', 0, 1, 4),
    (291, 29, 'A', '现有竞争者', 1, 1, 1),
    (292, 29, 'B', '客户与供应商', 1, 1, 2),
    (293, 29, 'C', '替代方案', 1, 1, 3),
    (294, 29, 'D', '潜在进入者', 1, 1, 4),
    (301, 30, 'TRUE', '正确', 0, 1, 1),
    (302, 30, 'FALSE', '错误', 1, 1, 2),
    (311, 31, 'A', '长期稳定的复购率', 1, 1, 1),
    (312, 31, 'B', '一次广告曝光', 0, 1, 2),
    (313, 31, 'C', '管理层一句口号', 0, 1, 3),
    (314, 31, 'D', '单日价格上涨', 0, 1, 4),
    (321, 32, 'A', '品牌知名度', 1, 1, 1),
    (322, 32, 'B', '管理层自我评价', 1, 1, 2),
    (323, 32, 'C', '短期高利润', 1, 1, 3),
    (324, 32, 'D', '长期客户与经营数据的共同支持', 0, 1, 4),
    (331, 33, 'TRUE', '正确', 0, 1, 1),
    (332, 33, 'FALSE', '错误', 1, 1, 2),
    (341, 34, 'A', '风险来源', 1, 1, 1),
    (342, 34, 'B', '行业驱动', 1, 1, 2),
    (343, 34, 'C', '地域暴露', 1, 1, 3),
    (344, 34, 'D', '只看持有项目数量', 0, 1, 4),
    (351, 35, 'A', '资金用途、期限、缓冲与心理感受', 1, 1, 1),
    (352, 35, 'B', '最近一次收益', 0, 1, 2),
    (353, 35, 'C', '他人的风险偏好', 0, 1, 3),
    (354, 35, 'D', '资产名称是否热门', 0, 1, 4),
    (361, 36, 'TRUE', '正确', 0, 1, 1),
    (362, 36, 'FALSE', '错误', 1, 1, 2),
    (371, 37, 'A', '可核对的事实', 1, 1, 1),
    (372, 37, 'B', '尚待验证的假设', 1, 1, 2),
    (373, 37, 'C', '支持与反面证据', 1, 1, 3),
    (374, 37, 'D', '主要风险', 1, 1, 4),
    (381, 38, 'A', '写明可观察信号和复核日期', 1, 1, 1),
    (382, 38, 'B', '只写最终结论', 0, 1, 2),
    (383, 38, 'C', '删除所有不确定性', 0, 1, 3),
    (384, 38, 'D', '承诺假设一定成立', 0, 1, 4),
    (391, 39, 'TRUE', '正确', 0, 1, 1),
    (392, 39, 'FALSE', '错误', 1, 1, 2),
    (401, 40, 'A', '信息质量', 1, 1, 1),
    (402, 40, 'B', '推理过程', 1, 1, 2),
    (403, 40, 'C', '风险控制', 1, 1, 3),
    (404, 40, 'D', '结果与当时条件', 1, 1, 4);
