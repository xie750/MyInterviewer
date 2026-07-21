ALTER TABLE interviewer_style
    ADD COLUMN virtual_human_key VARCHAR(80) NULL AFTER scenario,
    ADD COLUMN virtual_human_name VARCHAR(80) NULL AFTER virtual_human_key,
    ADD COLUMN virtual_human_description VARCHAR(300) NULL AFTER virtual_human_name;

UPDATE interviewer_style
SET virtual_human_key = 'stern-panel',
    virtual_human_name = '冷静追问官',
    virtual_human_description = '节奏紧凑，关注风险、证据和边界条件。'
WHERE name = '严厉压力面';

UPDATE interviewer_style
SET virtual_human_key = 'warm-guide',
    virtual_human_name = '引导型导师',
    virtual_human_description = '语气温和，帮助候选人逐步展开经历和思路。'
WHERE name = '温和引导面';

UPDATE interviewer_style
SET virtual_human_key = 'tech-architect',
    virtual_human_name = '架构深挖官',
    virtual_human_description = '聚焦技术原理、系统设计和工程取舍。'
WHERE name = '技术深挖面';

UPDATE interviewer_style
SET virtual_human_key = 'hr-partner',
    virtual_human_name = '综合评估官',
    virtual_human_description = '关注表达、协作、复盘和长期匹配度。'
WHERE name = 'HR 综合面';

UPDATE interviewer_style
SET virtual_human_key = 'challenge-master',
    virtual_human_name = '挑战面试官',
    virtual_human_description = '通过复杂场景和突发问题检验应变能力。'
WHERE name = '挑战面试官';

UPDATE interviewer_style
SET virtual_human_key = 'practice-coach',
    virtual_human_name = '实战演练官',
    virtual_human_description = '结合实际项目案例评估实战能力。'
WHERE name = '实战演练官';
