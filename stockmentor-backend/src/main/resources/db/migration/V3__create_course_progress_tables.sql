CREATE TABLE course (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    cover_url VARCHAR(500) NULL,
    published TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course_published_sort (published, sort_order, id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chapter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_chapter_course_sort (course_id, sort_order),
    CONSTRAINT fk_chapter_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE lesson (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content_md TEXT NOT NULL,
    estimated_minutes SMALLINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL,
    published TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_chapter_sort (chapter_id, sort_order),
    KEY idx_lesson_chapter_published_sort (
        chapter_id,
        published,
        sort_order,
        id
    ),
    CONSTRAINT chk_lesson_estimated_minutes CHECK (estimated_minutes > 0),
    CONSTRAINT fk_lesson_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapter (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_lesson_progress (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    completed_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_lesson_progress_user_lesson (user_id, lesson_id),
    KEY idx_user_lesson_progress_lesson (lesson_id),
    CONSTRAINT fk_user_lesson_progress_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_lesson_progress_lesson
        FOREIGN KEY (lesson_id) REFERENCES lesson (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO course (
    id,
    title,
    summary,
    cover_url,
    published,
    sort_order
) VALUES (
    1,
    '股票投资基础',
    '从基本资产、财务报表到风险复盘，建立清晰的投资学习框架。',
    NULL,
    1,
    1
);

INSERT INTO chapter (id, course_id, title, summary, sort_order) VALUES
    (1, 1, '股票、基金、债券和指数', '认识常见资产与市场描述工具。', 1),
    (2, 1, '收益与风险', '理解收益来源与风险的多种表现。', 2),
    (3, 1, '公司和商业模式', '从价值创造过程认识一家公司。', 3),
    (4, 1, '利润表', '读懂经营成果及其质量。', 4),
    (5, 1, '资产负债表', '观察资源来源、结构与偿债压力。', 5),
    (6, 1, '现金流量表', '理解现金流入流出与利润的差异。', 6),
    (7, 1, '常见估值指标', '认识指标含义以及适用边界。', 7),
    (8, 1, '行业与竞争分析', '用结构和证据观察竞争环境。', 8),
    (9, 1, '风险和分散', '建立与自身承受能力相符的风险意识。', 9),
    (10, 1, '投资复盘', '用记录和复核改善分析过程。', 10);

INSERT INTO lesson (
    id,
    chapter_id,
    title,
    summary,
    content_md,
    estimated_minutes,
    sort_order,
    published
) VALUES
    (
        1,
        1,
        '股票、基金与债券的基本权利',
        '区分三类常见资产所代表的基本权利。',
        '## 概念
股票通常代表公司所有权的一部分，基金代表一篮子资产的份额，债券通常代表约定偿付的债权。

## 简单例子
同样投入一笔学习资金，三类资产对应的收益来源、参与方式和风险承担并不相同。

## 常见误区
名称熟悉不等于风险相同，也不能仅凭历史表现判断未来结果。

## 学习小结
先辨认持有的权利，再讨论收益、价格和风险。',
        8,
        1,
        1
    ),
    (
        2,
        1,
        '指数如何描述一组资产',
        '理解指数的样本、权重与比较用途。',
        '## 概念
指数按照既定规则选择样本并计算综合变化，用来描述一组资产的整体表现。

## 简单例子
若权重较大的成分变化明显，它对加权指数的影响通常也更大。

## 常见误区
指数上涨不表示每个成分都上涨，指数本身也不是一份完整研究结论。

## 学习小结
阅读指数时要同时了解样本范围、加权方法和调整规则。',
        7,
        2,
        1
    ),
    (
        3,
        2,
        '投资收益从哪里来',
        '拆分价格变化、现金分配与持有成本。',
        '## 概念
投资结果可能来自价格变化和现金分配，同时还会受到费用、税费与时间的影响。

## 简单例子
账面价格上升并不等于全部净收益，还需要扣除实际发生的持有与交易成本。

## 常见误区
只看单一时期的涨幅，容易忽略现金流和投入时间的差别。

## 学习小结
比较收益前先统一口径、期间和成本范围。',
        8,
        1,
        1
    ),
    (
        4,
        2,
        '风险不只是价格波动',
        '从多种角度认识不确定性与损失来源。',
        '## 概念
风险包括价格波动，也包括流动性不足、信用变化、经营恶化和判断依据失效。

## 简单例子
价格暂时平稳的资产，也可能因为信息不足或难以及时交易而存在风险。

## 常见误区
低波动不必然等于低风险，高波动也不能单独说明长期价值。

## 学习小结
用风险清单补充单一波动指标。',
        8,
        2,
        1
    ),
    (
        5,
        3,
        '公司如何创造价值',
        '认识投入、活动、客户价值与回报之间的联系。',
        '## 概念
公司把人才、资本和资源组织成产品或服务，并通过满足客户需求形成收入。

## 简单例子
一家服务企业可能通过提升效率和复购率，让相同资源服务更多客户。

## 常见误区
收入增长不等于价值一定增长，还要观察成本、投入和持续性。

## 学习小结
沿着投入、活动、客户和回报四个环节理解价值创造。',
        9,
        1,
        1
    ),
    (
        6,
        3,
        '用商业模式框架理解公司',
        '用客户、产品、收入与成本梳理经营逻辑。',
        '## 概念
商业模式说明公司服务谁、提供什么、如何交付，以及收入和成本怎样形成。

## 简单例子
订阅模式强调持续服务和续费，单次销售则更依赖不断获得新订单。

## 常见误区
只描述产品功能，不能替代对客户需求和经济结构的分析。

## 学习小结
把经营叙述转化为可观察、可验证的关键环节。',
        9,
        2,
        1
    ),
    (
        7,
        4,
        '利润表的收入、成本与利润',
        '掌握利润表的基本层次与相互关系。',
        '## 概念
利润表在一定期间内汇总收入和各类成本费用，最终得到利润结果。

## 简单例子
收入增加时，若成本费用增长更快，利润仍可能下降。

## 常见误区
利润表是期间记录，不能直接当作期末现金余额或资产清单。

## 学习小结
按收入、成本费用和利润的顺序阅读经营成果。',
        9,
        1,
        1
    ),
    (
        8,
        4,
        '识别利润质量和一次性项目',
        '区分持续经营贡献与偶发影响。',
        '## 概念
利润质量关注利润是否来自可持续经营，并能否得到现金和业务证据支持。

## 简单例子
出售一项资产可能抬高当期利润，但这种来源未必会在下一期重复。

## 常见误区
看到利润增长就外推未来，容易忽略一次性项目和会计估计变化。

## 学习小结
追问利润来源、重复性和现金支持。',
        9,
        2,
        1
    ),
    (
        9,
        5,
        '资产、负债与所有者权益',
        '理解资产负债表的基本恒等关系。',
        '## 概念
资产是企业控制的资源，负债是需要履行的义务，所有者权益是剩余权益。

## 简单例子
企业借款购置设备时，资产和负债可能同时增加。

## 常见误区
资产金额大不表示资产质量一定高，也不代表随时可以变现。

## 学习小结
结合资源质量、资金来源和期限结构阅读报表。',
        9,
        1,
        1
    ),
    (
        10,
        5,
        '从资产负债表观察偿债风险',
        '从期限、流动性与负债结构识别压力。',
        '## 概念
偿债风险取决于到期义务、可用现金、资产变现能力和持续融资条件。

## 简单例子
短期负债集中到期，而流动资产难以变现时，资金压力可能上升。

## 常见误区
只看总负债比例，会遗漏到期时间、利率和币种等重要差异。

## 学习小结
把负债规模与期限、现金和经营稳定性一起观察。',
        9,
        2,
        1
    ),
    (
        11,
        6,
        '经营、投资与筹资现金流',
        '区分三类现金活动及其含义。',
        '## 概念
现金流量表把现金变化分为经营、投资和筹资活动，展示资金从哪里来、到哪里去。

## 简单例子
购置设备通常归入投资活动，借款收到现金通常归入筹资活动。

## 常见误区
单看现金净增加额，可能掩盖经营流出由新增借款弥补的结构。

## 学习小结
分别查看三类现金流，再分析它们之间的组合。',
        9,
        1,
        1
    ),
    (
        12,
        6,
        '利润与现金为什么不同',
        '理解权责发生制与现金收付的时间差。',
        '## 概念
利润按收入和费用的确认规则计算，现金流记录实际收付，两者时间可能不同。

## 简单例子
赊销可以确认收入，但客户尚未付款时并不会同步收到现金。

## 常见误区
利润与经营现金短期不一致并非必然异常，需要结合原因和持续时间判断。

## 学习小结
用应收、存货和预付款等变化解释利润现金差异。',
        9,
        2,
        1
    ),
    (
        13,
        7,
        '市盈率表达了什么',
        '理解价格与每股盈利之间的相对关系。',
        '## 概念
市盈率把市场价格与盈利口径联系起来，便于在条件相近时进行相对观察。

## 简单例子
两家公司市盈率不同，可能反映增长预期、风险和盈利稳定性的差异。

## 常见误区
较低的市盈率不自动意味着便宜，盈利异常或下降会扭曲指标。

## 学习小结
核对盈利口径，并结合增长、质量与风险解释市盈率。',
        8,
        1,
        1
    ),
    (
        14,
        7,
        '市净率等指标的适用边界',
        '根据业务特征选择并解释相对指标。',
        '## 概念
市净率连接价格与账面净资产，其他指标也各自强调不同经营变量。

## 简单例子
资产结构差异很大的企业，即使市净率相近，含义也可能不同。

## 常见误区
用一个指标跨行业直接排序，会忽略会计口径和商业模式差异。

## 学习小结
先确认指标与业务是否匹配，再做有限范围的比较。',
        8,
        2,
        1
    ),
    (
        15,
        8,
        '行业结构与竞争强度',
        '观察参与者、替代品和议价关系。',
        '## 概念
行业结构由竞争者、客户、供应商、潜在进入者和替代方案共同塑造。

## 简单例子
进入门槛较低且产品相似时，参与者可能更容易陷入价格竞争。

## 常见误区
行业规模大不等于每个参与者都能获得良好回报。

## 学习小结
用多方关系解释竞争强度，而不是只统计公司数量。',
        9,
        1,
        1
    ),
    (
        16,
        8,
        '竞争优势需要哪些证据',
        '用经营数据检验优势是否真实且持续。',
        '## 概念
竞争优势是企业持续创造差异并抵御竞争的能力，需要客户和经营结果支持。

## 简单例子
较高复购率若长期稳定，可能为客户黏性提供一项证据。

## 常见误区
品牌知名度、管理层表述或短期高利润都不能单独证明持久优势。

## 学习小结
为每个优势假设寻找可跟踪、可反驳的证据。',
        9,
        2,
        1
    ),
    (
        17,
        9,
        '单一风险与分散原则',
        '理解分散如何降低特定风险及其限制。',
        '## 概念
分散是把暴露分布到不同来源，减少单一事件对整体结果的影响。

## 简单例子
若多个项目都依赖同一种需求，它们数量虽多，实际风险来源仍可能集中。

## 常见误区
分散不能消除所有市场风险，也不是简单增加持有数量。

## 学习小结
检查风险来源之间是否真正不同，并认识分散的边界。',
        8,
        1,
        1
    ),
    (
        18,
        9,
        '风险承受能力与学习边界',
        '区分财务能力、心理感受与知识范围。',
        '## 概念
风险承受能力受资金用途、时间安排、财务缓冲和面对波动的感受共同影响。

## 简单例子
近期需要使用的资金，与长期闲置资金能够承受的变化通常不同。

## 常见误区
一次顺利经历不能证明承受能力，也不能替代对产品和风险的理解。

## 学习小结
明确资金期限、最大可承受损失和暂不理解的领域。',
        8,
        2,
        1
    ),
    (
        19,
        10,
        '记录假设、证据与风险',
        '用结构化记录保存当时的判断依据。',
        '## 概念
复盘记录应区分事实、假设、推论和风险，保留判断形成时的信息环境。

## 简单例子
记录收入增长假设时，同时写下支持数据、反面信号和检查日期。

## 常见误区
只记录结论会让后续难以判断错误来自信息、逻辑还是执行。

## 学习小结
让每项判断都有来源、条件和可检验的后续信号。',
        8,
        1,
        1
    ),
    (
        20,
        10,
        '复盘过程而不只看结果',
        '把结果与当时可用信息和决策过程分开评价。',
        '## 概念
复盘关注过程是否合理，因为短期结果可能同时受到判断和偶然因素影响。

## 简单例子
结果理想但依据薄弱的判断，仍需要记录并改进分析步骤。

## 常见误区
用一次结果给过程贴上好坏标签，会产生结果偏差。

## 学习小结
分别评价信息质量、推理过程、风险控制和最终结果。',
        8,
        2,
        1
    );
