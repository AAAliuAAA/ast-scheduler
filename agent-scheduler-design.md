# Agent 调度中心架构设计

## 一、系统概述

### 1.1 项目定位
个人 Agent 统一调度管理平台，类似简化版 XXL-Job，专注于管理 OpenClaw、Claude Code 等 AI Agent 的任务调度。

### 1.2 核心目标
- **简单易用**：零学习成本，打开即用
- **轻量级**：单机部署，无需复杂配置
- **功能完整**：覆盖日常调度需求

### 1.3 技术栈
- **后端**：Spring Boot 3.x + MyBatis
- **数据库**：SQLite（默认，零配置）/ MySQL（可选，团队部署）
- **前端**：Thymeleaf + Bootstrap 5（简单直观）
- **调度**：Spring Task Scheduler
- **通知**：邮件/企业微信/钉钉

---

## 二、核心功能模块

### 2.1 执行器注册中心
```
功能点：
- 执行器注册（手动配置）
- 执行器列表展示
- 健康检测（主动下发测试消息）
- 执行器状态监控（在线/离线/异常）
- 执行器分组管理
```

### 2.2 任务管理
```
功能点：
- 创建任务（名称、描述、Cron表达式、Agent类型）
- 编辑任务
- 启用/禁用任务
- 删除任务
- 手动触发执行
```

### 2.2 Agent 执行器管理
```
支持的 Agent 类型：
- Claude Code（通过 CLI 调用）
- OpenClaw（通过 API/CLI 调用）
- 自定义脚本（Shell/Python）
```

### 2.3 调度引擎
```
- Cron 表达式解析
- 定时触发
- 超时控制
- 任务依赖（前置任务完成后执行）
```

### 2.4 执行日志
```
- 实时日志输出
- 日志归档（按天/按任务）
- 日志清理（保留最近 N 天）
- 日志搜索
```

### 2.5 监控告警
```
- 任务执行状态监控
- 失败告警（邮件/webhook）
- 超时告警
- 执行统计（成功率、平均耗时）
```

---

## 三、数据库设计

### 3.1 执行器表 (t_executor)
```sql
-- 执行器注册表
CREATE TABLE t_executor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    executor_name VARCHAR(100) NOT NULL,  -- 执行器名称
    executor_type VARCHAR(50) NOT NULL,   -- 执行器类型：CLAUDE_CODE/OPENCLAW/SCRIPT
    executor_desc VARCHAR(500),           -- 执行器描述
    connection_type VARCHAR(50) NOT NULL, -- 连接类型：CLI/API/SSH
    connection_config TEXT,               -- 连接配置JSON
    health_check_command TEXT,            -- 健康检查命令
    status INTEGER DEFAULT 0,             -- 状态：0-未知 1-在线 2-离线 3-异常
    is_favorite INTEGER DEFAULT 0,        -- 是否收藏：0-否 1-是
    favorite_order INTEGER DEFAULT 0,     -- 收藏排序
    last_check_time DATETIME,             -- 最后检测时间
    last_check_result TEXT,               -- 最后检测结果
    group_name VARCHAR(50) DEFAULT 'default', -- 执行器分组
    sort_order INTEGER DEFAULT 0,         -- 排序
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_executor_type ON t_executor(executor_type);
CREATE INDEX idx_executor_status ON t_executor(status);
CREATE INDEX idx_executor_favorite ON t_executor(is_favorite);
CREATE INDEX idx_executor_group ON t_executor(group_name);

-- 自动更新 update_time 触发器
CREATE TRIGGER update_executor_timestamp
AFTER UPDATE ON t_executor
BEGIN
    UPDATE t_executor SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
```

### 3.2 任务表 (t_task)
```sql
-- 任务配置表
CREATE TABLE t_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_name VARCHAR(100) NOT NULL,      -- 任务名称
    task_desc VARCHAR(500),               -- 任务描述
    executor_id INTEGER NOT NULL,         -- 执行器ID
    prompt_template TEXT,                 -- Prompt模板
    agent_role VARCHAR(200),              -- Agent角色定义
    agent_rules TEXT,                     -- Agent规则限制
    command TEXT NOT NULL,                -- 最终执行命令（自动生成）
    cron_expression VARCHAR(100),         -- Cron表达式
    timeout_seconds INTEGER DEFAULT 3600, -- 超时时间（秒）
    retry_count INTEGER DEFAULT 0,        -- 重试次数
    status INTEGER DEFAULT 1,             -- 状态：0-禁用 1-启用
    depend_task_id INTEGER,               -- 依赖任务ID
    alarm_email VARCHAR(200),             -- 告警邮箱
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_status ON t_task(status);
CREATE INDEX idx_task_executor_id ON t_task(executor_id);

-- 自动更新 update_time 触发器
CREATE TRIGGER update_task_timestamp
AFTER UPDATE ON t_task
BEGIN
    UPDATE t_task SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
```

### 3.2 执行日志表 (t_task_log)
```sql
-- 任务执行日志表
CREATE TABLE t_task_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,             -- 任务ID
    trigger_type INTEGER,                 -- 触发类型：1-定时 2-手动 3-依赖
    start_time DATETIME,                  -- 开始时间
    end_time DATETIME,                    -- 结束时间
    duration_ms INTEGER,                  -- 执行时长（毫秒）
    status INTEGER,                       -- 状态：1-执行中 2-成功 3-失败 4-超时
    log_content TEXT,                     -- 日志内容
    error_msg TEXT,                       -- 错误信息
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_task_id ON t_task_log(task_id);
CREATE INDEX idx_log_start_time ON t_task_log(start_time);
CREATE INDEX idx_log_status ON t_task_log(status);
```

### 3.3 Prompt 模板表 (t_prompt_template)
```sql
-- Prompt模板表
CREATE TABLE t_prompt_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_name VARCHAR(100) NOT NULL,  -- 模板名称
    template_desc VARCHAR(500),           -- 模板描述
    agent_type VARCHAR(50),               -- 适用Agent类型
    role_definition TEXT,                 -- 角色定义
    rules_template TEXT,                  -- 规则模板
    prompt_template TEXT,                 -- Prompt模板
    variables VARCHAR(500),               -- 变量列表（JSON）
    is_system INTEGER DEFAULT 0,          -- 是否系统模板
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_template_agent_type ON t_prompt_template(agent_type);

-- 自动更新 update_time 触发器
CREATE TRIGGER update_prompt_template_timestamp
AFTER UPDATE ON t_prompt_template
BEGIN
    UPDATE t_prompt_template SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
```

### 3.4 告警配置表 (t_alarm_config)
```sql
-- 告警配置表
CREATE TABLE t_alarm_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_name VARCHAR(100) NOT NULL,    -- 配置名称
    alarm_type VARCHAR(50) NOT NULL,      -- 告警类型：EMAIL/WECHAT/DINGTALK
    config_json TEXT,                     -- 配置JSON
    is_default INTEGER DEFAULT 0,         -- 是否默认配置
    status INTEGER DEFAULT 1,             -- 状态：0-禁用 1-启用
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 自动更新 update_time 触发器
CREATE TRIGGER update_alarm_config_timestamp
AFTER UPDATE ON t_alarm_config
BEGIN
    UPDATE t_alarm_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
```

### 3.5 用户配置表 (t_user_config)
```sql
-- 用户配置表
CREATE TABLE t_user_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(100) NOT NULL,       -- 用户名
    language VARCHAR(10) DEFAULT 'zh',    -- 语言：zh-中文 en-英文
    theme VARCHAR(20) DEFAULT 'light',    -- 主题：light-明色 dark-暗色
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 自动更新 update_time 触发器
CREATE TRIGGER update_user_config_timestamp
AFTER UPDATE ON t_user_config
BEGIN
    UPDATE t_user_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
```

---

## 四、系统架构

### 4.1 分层架构
```
┌─────────────────────────────────────────┐
│         Web Layer (Controller)          │
│  - 任务管理页面                          │
│  - 日志查看页面                          │
│  - 监控大盘                              │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│       Service Layer (业务逻辑)           │
│  - TaskService                          │
│  - SchedulerService                     │
│  - LogService                           │
│  - AlarmService                         │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│      Executor Layer (执行器)             │
│  - ClaudeCodeExecutor                   │
│  - OpenClawExecutor                     │
│  - ScriptExecutor                       │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         Data Layer (MyBatis)            │
│  - TaskMapper                           │
│  - TaskLogMapper                        │
│  - AlarmConfigMapper                    │
└─────────────────────────────────────────┘
```

### 4.2 核心组件

#### 4.2.1 调度管理器 (SchedulerManager)
```java
职责：
- 启动时加载所有启用的任务
- 根据 Cron 表达式注册定时任务
- 任务状态变更时动态更新调度
- 处理任务依赖关系
```

#### 4.2.2 执行器接口 (AgentExecutor)
```java
public interface AgentExecutor {
    ExecuteResult execute(Task task, TaskLog taskLog);
    boolean support(String agentType);
    void cancel(Long logId);
}
```

#### 4.2.3 日志管理器 (LogManager)
```java
职责：
- 实时写入执行日志
- 定时归档历史日志
- 定时清理过期日志
- 提供日志查询接口
```

#### 4.2.4 告警管理器 (AlarmManager)
```java
职责：
- 任务失败告警
- 任务超时告警
- 发送告警通知（邮件/webhook）
```

---

## 五、核心流程

### 5.1 任务调度流程
```
1. SchedulerManager 根据 Cron 触发任务
2. 检查依赖任务是否完成
3. 创建 TaskLog 记录
4. 根据 agentType 选择对应的 Executor
5. 异步执行任务（带超时控制）
6. 实时写入日志
7. 更新执行结果
8. 失败时发送告警
```

### 5.2 手动触发流程
```
1. 用户点击"立即执行"
2. 检查任务是否正在执行
3. 创建 TaskLog（触发类型=手动）
4. 执行任务（同调度流程）
```

### 5.3 日志清理流程
```
1. 每天凌晨 2 点执行
2. 查询超过保留期的日志
3. 归档到文件（可选）
4. 删除数据库记录
```

---

## 六、页面设计（简洁版）

### 6.1 首页 - 执行器注册中心
```
┌────────────────────────────────────────────────┐
│ Agent 调度中心                   [+ 注册执行器]  │
├────────────────────────────────────────────────┤
│ 执行器列表                                      │
│ 分组: [全部▼]  类型: [全部▼]  状态: [全部▼]     │
├────┬──────┬────────┬──────┬──────┬──────┬─────┤
│状态│ 名称 │ 类型   │ 分组 │ 最后 │ 操作 │     │
│    │      │        │      │ 检测 │      │     │
├────┼──────┼────────┼──────┼──────┼──────┼─────┤
│ 🟢 │Claude│Claude  │AI    │2分钟 │[检测]│     │
│    │Code  │Code    │Agent │前    │[编辑]│     │
│    │本地  │        │      │      │[删除]│     │
├────┼──────┼────────┼──────┼──────┼──────┼─────┤
│ 🔴 │Open  │OpenClaw│AI    │1小时 │[检测]│     │
│    │Claw  │        │Agent │前    │[编辑]│     │
│    │远程  │        │      │      │[删除]│     │
├────┼──────┼────────┼──────┼──────┼──────┼─────┤
│ 🟡 │Python│Script  │脚本  │未检测│[检测]│     │
│    │脚本  │        │      │      │[编辑]│     │
│    │      │        │      │      │[删除]│     │
└────┴──────┴────────┴──────┴──────┴──────┴─────┘

状态说明：
🟢 在线  🔴 离线  🟡 未知  🟠 异常
```

### 6.2 注册执行器页面
```
┌────────────────────────────────────────────────┐
│ 注册执行器                            [保存] [取消]│
├────────────────────────────────────────────────┤
│ 基本信息                                        │
│ 执行器名称: [___________________________]      │
│ 执行器描述: [___________________________]      │
│ 执行器类型: [Claude Code ▼]                   │
│ 执行器分组: [AI Agent ▼]                      │
│                                                │
│ 连接配置                                        │
│ 连接方式: [CLI ▼]                             │
│ 命令路径: [kiro-cli]                          │
│ 工作目录: [/home/user/workspace]              │
│                                                │
│ 健康检查                                        │
│ 检测命令: [kiro-cli chat "ping"]              │
│ 超时时间: [30] 秒                              │
│                                                │
│ [测试连接]                                      │
└────────────────────────────────────────────────┘
```

### 6.3 任务列表页
```
┌────────────────────────────────────────────────┐
│ 任务管理                          [+ 新建任务]  │
├────────────────────────────────────────────────┤
│ 搜索: [_______]  执行器: [全部▼]  状态: [全部▼] │
├────┬──────┬────────┬──────┬──────┬──────┬─────┤
│状态│ 名称 │ 执行器 │ Cron │ 操作 │ 最近 │ 成功│
│    │      │        │      │      │ 执行 │ 率  │
├────┼──────┼────────┼──────┼──────┼──────┼─────┤
│ ●  │代码  │Claude  │0 9 * │[执行]│2分钟 │ 98% │
│    │审查  │Code本地│* * * │[编辑]│前    │     │
│    │      │        │      │[日志]│      │     │
│    │      │        │      │[停用]│      │     │
├────┼──────┼────────┼──────┼──────┼──────┼─────┤
│ ○  │文档  │OpenClaw│0 */2 │[执行]│1小时 │ 100%│
│    │生成  │远程    │* * * │[编辑]│前    │     │
│    │      │        │      │[日志]│      │     │
│    │      │        │      │[启用]│      │     │
└────┴──────┴────────┴──────┴──────┴──────┴─────┘
```

### 6.4 新建/编辑任务页（增强版）
```
┌────────────────────────────────────────────────┐
│ 新建任务                              [保存] [取消]│
├────────────────────────────────────────────────┤
│ 基本信息                                        │
│ 任务名称: [___________________________]        │
│ 任务描述: [___________________________]        │
│                                                │
│ 执行器配置                                      │
│ 选择执行器: [Claude Code本地 ▼] 🟢在线        │
│                                                │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ Prompt 工程配置  [使用模板▼] [AI辅助生成]      │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                │
│ 1️⃣ Agent 角色定义                              │
│ ┌──────────────────────────────────────────┐ │
│ │你是一个专业的代码审查助手，擅长发现代码中的│ │
│ │潜在问题、性能瓶颈和安全漏洞。            │ │
│ └──────────────────────────────────────────┘ │
│ 💡 提示：明确定义 Agent 的专业领域和能力边界   │
│                                                │
│ 2️⃣ 规则与限制                                  │
│ ┌──────────────────────────────────────────┐ │
│ │- 只审查 src/ 目录下的代码                │ │
│ │- 忽略测试文件和配置文件                  │ │
│ │- 重点关注安全问题和性能问题              │ │
│ │- 每个问题必须给出具体的修改建议          │ │
│ │- 不要修改代码，只提供审查报告            │ │
│ └──────────────────────────────────────────┘ │
│ 💡 提示：设置明确的边界，避免 Agent 越权操作    │
│                                                │
│ 3️⃣ Prompt 模板                                 │
│ ┌──────────────────────────────────────────┐ │
│ │请审查最近的代码变更：                    │ │
│ │                                          │ │
│ │1. 检查是否存在安全漏洞（SQL注入、XSS等） │ │
│ │2. 分析性能瓶颈                           │ │
│ │3. 检查代码规范                           │ │
│ │                                          │ │
│ │输出格式：                                │ │
│ │- 问题描述                                │ │
│ │- 严重程度（高/中/低）                    │ │
│ │- 修改建议                                │ │
│ │- 相关文件和行号                          │ │
│ └──────────────────────────────────────────┘ │
│ 💡 提示：使用结构化 Prompt，明确输入输出格式    │
│                                                │
│ [预览最终命令]                                  │
│ ┌──────────────────────────────────────────┐ │
│ │kiro-cli chat "你是一个专业的代码审查助手...│ │
│ │请审查最近的代码变更..."                  │ │
│ └──────────────────────────────────────────┘ │
│                                                │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                │
│ 调度配置                                        │
│ Cron表达式: [0 9 * * *]  [?帮助]              │
│ 说明: 每天上午9点执行                           │
│                                                │
│ 高级配置                                        │
│ 超时时间: [3600] 秒                            │
│ 重试次数: [0] 次                               │
│ 依赖任务: [无 ▼]                               │
│                                                │
│ 告警配置                                        │
│ 告警邮箱: [user@example.com]                   │
│ □ 失败时告警  □ 超时告警                        │
└────────────────────────────────────────────────┘
```

### 6.5 Prompt 模板库页面
```
┌────────────────────────────────────────────────┐
│ Prompt 模板库                     [+ 新建模板]  │
├────────────────────────────────────────────────┤
│ 类型: [全部▼]  搜索: [_______]                 │
├────┬──────────┬────────┬──────────┬──────┬────┤
│标签│ 模板名称 │ 适用   │ 描述     │ 操作 │    │
├────┼──────────┼────────┼──────────┼──────┼────┤
│🏷️系统│代码审查  │Claude  │专业代码  │[使用]│    │
│    │          │Code    │审查助手  │[编辑]│    │
├────┼──────────┼────────┼──────────┼──────┼────┤
│🏷️系统│文档生成  │OpenClaw│自动生成  │[使用]│    │
│    │          │        │技术文档  │[编辑]│    │
├────┼──────────┼────────┼──────────┼──────┼────┤
│👤自定义│测试生成│Claude  │单元测试  │[使用]│    │
│    │          │Code    │生成器    │[删除]│    │
└────┴──────────┴────────┴──────────┴──────┴────┘

系统预置模板：
- 代码审查助手
- 文档生成助手
- Bug 分析助手
- 性能优化助手
- 安全扫描助手
```

### 6.8 执行器详情页
```
┌────────────────────────────────────────────────┐
│ ← 返回    Claude Code 本地    🟢在线  [⭐已收藏]│
├────────────────────────────────────────────────┤
│ 当前执行任务                                    │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ 🔄 代码审查任务 (执行中) - 已运行：45秒        │
│                                                │
│ 实时输出：                                      │
│ ┌──────────────────────────────────────────┐ │
│ │ [09:15:23] 开始分析代码...               │ │
│ │ [09:15:25] 读取文件：src/main/User.java │ │
│ │ [09:15:27] 发现潜在问题：                │ │
│ │   - 第45行：未处理的异常                 │ │
│ │   - 第78行：SQL注入风险                  │ │
│ │ [09:15:30] 正在生成详细报告...           │ │
│ │ [09:15:35] ▊                             │ │
│ │                                          │ │
│ │ [自动滚动] [暂停] [复制]                 │ │
│ └──────────────────────────────────────────┘ │
│                                                │
│ 最近执行任务                                    │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ ┌────┬──────────┬──────┬──────┬──────┬─────┐ │
│ │状态│ 任务名称 │ 开始 │ 耗时 │ 触发 │操作 │ │
│ ├────┼──────────┼──────┼──────┼──────┼─────┤ │
│ │🔄  │代码审查  │09:15 │ 45s  │定时  │[详情]│ │
│ │✓   │代码审查  │09:00 │ 38s  │定时  │[日志]│ │
│ │✓   │文档生成  │08:30 │ 125s │手动  │[日志]│ │
│ │✗   │代码审查  │08:00 │ 180s │定时  │[日志]│ │
│ └────┴──────────┴──────┴──────┴──────┴─────┘ │
└────────────────────────────────────────────────┘
```

### 6.9 日志查看页
```
┌────────────────────────────────────────────────┐
│ 任务日志 - 代码审查                             │
├────────────────────────────────────────────────┤
│ 时间范围: [今天▼]  状态: [全部▼]  [刷新]        │
├──────┬──────────┬──────┬──────┬──────┬────────┤
│状态  │ 开始时间  │ 耗时 │ 触发 │ 操作 │        │
├──────┼──────────┼──────┼──────┼──────┼────────┤
│ ✓成功│09:00:15  │ 45s  │定时  │[详情]│        │
├──────┼──────────┼──────┼──────┼──────┼────────┤
│ ✗失败│08:00:12  │ 120s │手动  │[详情]│        │
└──────┴──────────┴──────┴──────┴──────┴────────┘

日志详情:
┌────────────────────────────────────────────────┐
│ [09:00:15] 开始执行任务...                      │
│ [09:00:16] 调用 Claude Code                    │
│ [09:00:20] 正在分析代码...                      │
│ [09:00:45] 审查完成，发现 3 个问题              │
│ [09:01:00] 任务执行成功                         │
└────────────────────────────────────────────────┘
```

### 6.7 监控大盘（重新设计）
```
┌────────────────────────────────────────────────┐
│ 监控大盘                          [设置收藏▼]   │
├────────────────────────────────────────────────┤
│ 收藏的执行器（最多3个）                         │
│                                                │
│ ┌──────────────────────────────────────────┐ │
│ │ 🟢 Claude Code 本地        [详情] [取消收藏]│ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 🔄 正在执行：代码审查任务                │ │
│ │ ┌────────────────────────────────────┐ │ │
│ │ │ [09:15:23] 开始分析代码...         │ │ │
│ │ │ [09:15:25] 发现 3 个潜在问题       │ │ │
│ │ │ [09:15:27] 正在生成报告...         │ │ │
│ │ │ [09:15:30] ▊                       │ │ │
│ │ └────────────────────────────────────┘ │ │
│ │ 最近执行：2分钟前 | 今日成功：12/13      │ │
│ └──────────────────────────────────────────┘ │
│                                                │
│ ┌──────────────────────────────────────────┐ │
│ │ 🟢 OpenClaw 远程           [详情] [取消收藏]│ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ ⏸️  空闲中                                │ │
│ │ 最近执行：1小时前 | 今日成功：5/5        │ │
│ └──────────────────────────────────────────┘ │
│                                                │
│ ┌──────────────────────────────────────────┐ │
│ │ 🔴 Python 脚本             [详情] [取消收藏]│ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ ⚠️  离线 - 最后检测：30分钟前            │ │
│ │ 最近执行：2小时前 | 今日成功：0/1        │ │
│ └──────────────────────────────────────────┘ │
│                                                │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                │
│ 今日统计                                        │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐          │
│ │ 执行 │ │ 成功 │ │ 失败 │ │ 平均 │          │
│ │  24  │ │  22  │ │  2   │ │ 35s  │          │
│ └──────┘ └──────┘ └──────┘ └──────┘          │
└────────────────────────────────────────────────┘

说明：
- 点击执行器卡片可跳转到详情页
- 流式输出实时刷新（WebSocket）
- 最多收藏3个执行器
```

---

## 七、关键实现要点

### 7.1 Cron 调度实现
```java
@Component
public class DynamicScheduler {

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 注册任务
    public void registerTask(Task task) {
        CronTrigger trigger = new CronTrigger(task.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> executeTask(task),
            trigger
        );
        scheduledTasks.put(task.getId(), future);
    }

    // 取消任务
    public void cancelTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
```

### 7.2 超时控制
```java
@Service
public class TaskExecutorService {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void executeWithTimeout(Task task, TaskLog log) {
        Future<?> future = executor.submit(() -> {
            AgentExecutor executor = getExecutor(task.getAgentType());
            executor.execute(task, log);
        });

        try {
            future.get(task.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.setStatus(TaskStatus.TIMEOUT);
            alarmService.sendTimeoutAlarm(task, log);
        }
    }
}
```

### 7.3 任务依赖处理
```java
public boolean checkDependency(Task task) {
    if (task.getDependTaskId() == null) {
        return true;
    }

    // 查询依赖任务最近一次执行记录
    TaskLog lastLog = taskLogMapper.selectLastLog(task.getDependTaskId());

    // 依赖任务必须成功完成
    return lastLog != null && lastLog.getStatus() == TaskStatus.SUCCESS;
}
```

### 7.4 日志归档清理
```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
public void cleanOldLogs() {
    LocalDateTime expireTime = LocalDateTime.now().minusDays(30);

    // 查询过期日志
    List<TaskLog> oldLogs = taskLogMapper.selectByCreateTimeBefore(expireTime);

    // 归档到文件（可选）
    if (archiveEnabled) {
        archiveToFile(oldLogs);
    }

    // 删除数据库记录
    taskLogMapper.deleteByCreateTimeBefore(expireTime);

    log.info("清理了 {} 条过期日志", oldLogs.size());
}
```

---

## 八、执行器注册中心实现

### 8.1 Prompt 模板服务
```java
@Service
public class PromptTemplateService {

    @Autowired
    private PromptTemplateMapper templateMapper;

    // 系统预置模板
    @PostConstruct
    public void initSystemTemplates() {
        if (templateMapper.countSystemTemplates() == 0) {
            createSystemTemplate("代码审查助手", "CLAUDE_CODE",
                "你是一个专业的代码审查助手，擅长发现代码中的潜在问题、性能瓶颈和安全漏洞。",
                "- 只审查 src/ 目录下的代码\n- 重点关注安全和性能\n- 不要修改代码",
                "请审查最近的代码变更，输出问题描述、严重程度和修改建议。"
            );
            // 其他预置模板...
        }
    }

    // 构建最终命令
    public String buildCommand(Task task) {
        StringBuilder command = new StringBuilder();

        // 添加角色定义
        if (task.getAgentRole() != null) {
            command.append(task.getAgentRole()).append("\n\n");
        }

        // 添加规则限制
        if (task.getAgentRules() != null) {
            command.append("规则：\n").append(task.getAgentRules()).append("\n\n");
        }

        // 添加 Prompt
        command.append(task.getPromptTemplate());

        return command.toString();
    }
}
```

### 8.3 任务控制器（增强版）
```java
@Controller
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    // 保存任务
    @PostMapping("/save")
    @ResponseBody
    public Result save(@RequestBody Task task) {
        // 构建最终命令
        String finalCommand = promptTemplateService.buildCommand(task);
        task.setCommand(finalCommand);

        taskService.save(task);
        return Result.success();
    }

    // 获取模板列表
    @GetMapping("/templates")
    @ResponseBody
    public Result getTemplates(@RequestParam String agentType) {
        List<PromptTemplate> templates =
            promptTemplateService.listByAgentType(agentType);
        return Result.success(templates);
    }

    // AI 辅助生成 Prompt
    @PostMapping("/ai-generate-prompt")
    @ResponseBody
    public Result aiGeneratePrompt(@RequestBody PromptRequest request) {
        // 调用 AI 生成 Prompt
        String prompt = promptTemplateService.generateByAI(
            request.getTaskDesc(),
            request.getAgentType()
        );
        return Result.success(prompt);
    }
}
```

### 8.4 WebSocket 实时推送服务
```java
@Component
public class TaskLogWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String executorId = getExecutorId(session);
        sessions.put(executorId, session);
    }

    // 推送日志
    public void pushLog(Long executorId, String logLine) {
        WebSocketSession session = sessions.get(executorId.toString());
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(logLine));
            } catch (IOException e) {
                log.error("推送日志失败", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String executorId = getExecutorId(session);
        sessions.remove(executorId);
    }
}
```

### 8.5 任务执行服务（增强版）
```java
@Service
public class TaskExecutorService {

    @Autowired
    private WebSocketHandler webSocketHandler;

    private ExecuteResult executeByCLI(Executor executor, Task task, TaskLog log) {
        ProcessBuilder pb = new ProcessBuilder(/* ... */);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 写入数据库
                log.appendLog(line);

                // 实时推送到前端
                webSocketHandler.pushLog(executor.getId(), line);
            }
        }

        return ExecuteResult.success();
    }
}
```

### 8.6 执行器服务（增强版）
```java
@Data
public class Executor {
    private Long id;
    private String executorName;
    private String executorType;
    private String executorDesc;
    private String connectionType;
    private String connectionConfig;
    private String healthCheckCommand;
    private Integer status; // 0-未知 1-在线 2-离线 3-异常
    private LocalDateTime lastCheckTime;
    private String lastCheckResult;
    private String groupName;
    private Integer sortOrder;
}
```

### 8.2 执行器服务
```java
@Service
public class ExecutorService {

    @Autowired
    private ExecutorMapper executorMapper;

    // 注册执行器
    public void register(Executor executor) {
        executor.setStatus(0); // 未知状态
        executorMapper.insert(executor);
    }

    // 健康检测
    public HealthCheckResult healthCheck(Long executorId) {
        Executor executor = executorMapper.selectById(executorId);

        try {
            // 构建检测命令
            ProcessBuilder pb = new ProcessBuilder(
                parseCommand(executor.getHealthCheckCommand())
            );
            pb.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean success = process.waitFor(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // 更新状态
            executor.setStatus(success && process.exitValue() == 0 ? 1 : 3);
            executor.setLastCheckTime(LocalDateTime.now());
            executor.setLastCheckResult(String.format(
                "耗时: %dms\n输出: %s", duration, output.toString()
            ));
            executorMapper.updateById(executor);

            return HealthCheckResult.builder()
                .success(success && process.exitValue() == 0)
                .duration(duration)
                .output(output.toString())
                .build();

        } catch (Exception e) {
            executor.setStatus(2); // 离线
            executor.setLastCheckTime(LocalDateTime.now());
            executor.setLastCheckResult("检测失败: " + e.getMessage());
            executorMapper.updateById(executor);

            return HealthCheckResult.failure(e.getMessage());
        }
    }

    // 批量健康检测（定时任务）
    @Scheduled(cron = "0 */5 * * * ?") // 每5分钟
    public void batchHealthCheck() {
        List<Executor> executors = executorMapper.selectAll();
        for (Executor executor : executors) {
            healthCheck(executor.getId());
        }
    }
}
```

### 8.7 执行器控制器（增强版）
```java
@Controller
@RequestMapping("/executor")
public class ExecutorController {

    @Autowired
    private ExecutorService executorService;

    // 设置收藏
    @PostMapping("/favorite/{id}")
    @ResponseBody
    public Result setFavorite(@PathVariable Long id, @RequestParam boolean favorite) {
        executorService.setFavorite(id, favorite);
        return Result.success();
    }

    // 获取收藏的执行器
    @GetMapping("/favorites")
    @ResponseBody
    public Result getFavorites() {
        List<Executor> favorites = executorService.listFavorites();
        return Result.success(favorites);
    }

    // 执行器详情页
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Executor executor = executorService.getById(id);
        List<TaskLog> recentLogs = taskLogService.listByExecutor(id, 10);
        TaskLog runningTask = taskLogService.getRunningTask(id);

        model.addAttribute("executor", executor);
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("runningTask", runningTask);

        return "executor-detail";
    }
}
```

### 8.8 前端 WebSocket 连接
```javascript
// executor-detail.html
const ws = new WebSocket('ws://localhost:8080/ws/executor/' + executorId);

ws.onmessage = function(event) {
    const logLine = event.data;
    const logContainer = document.getElementById('log-output');
    logContainer.innerHTML += logLine + '\n';

    // 自动滚动
    if (!isPaused) {
        logContainer.scrollTop = logContainer.scrollHeight;
    }
};
```
```java
@Controller
@RequestMapping("/executor")
public class ExecutorController {

    @Autowired
    private ExecutorService executorService;

    // 执行器列表页
    @GetMapping("/list")
    public String list(Model model) {
        List<Executor> executors = executorService.listAll();
        model.addAttribute("executors", executors);
        return "executor-list";
    }

    // 注册页面
    @GetMapping("/add")
    public String addPage() {
        return "executor-form";
    }

    // 保存执行器
    @PostMapping("/save")
    @ResponseBody
    public Result save(@RequestBody Executor executor) {
        executorService.register(executor);
        return Result.success();
    }

    // 健康检测
    @PostMapping("/health-check/{id}")
    @ResponseBody
    public Result healthCheck(@PathVariable Long id) {
        HealthCheckResult result = executorService.healthCheck(id);
        return Result.success(result);
    }

    // 删除执行器
    @PostMapping("/delete/{id}")
    @ResponseBody
    public Result delete(@PathVariable Long id) {
        executorService.delete(id);
        return Result.success();
    }
}
```

---

## 九、任务执行实现（基于执行器注册中心）

### 9.1 任务执行服务
```java
@Service
public class TaskExecutorService {

    @Autowired
    private ExecutorService executorService;

    public void executeTask(Task task) {
        // 获取执行器配置
        Executor executor = executorService.getById(task.getExecutorId());

        // 检查执行器状态
        if (executor.getStatus() != 1) {
            throw new RuntimeException("执行器离线: " + executor.getExecutorName());
        }

        // 根据连接类型执行
        if ("CLI".equals(executor.getConnectionType())) {
            executeByCLI(executor, task);
        } else if ("API".equals(executor.getConnectionType())) {
            executeByAPI(executor, task);
        }
    }

    private void executeByCLI(Executor executor, Task task) {
        JSONObject config = JSON.parseObject(executor.getConnectionConfig());
        ProcessBuilder pb = new ProcessBuilder(
            config.getString("commandPath"), "chat", task.getCommand()
        );
        // 执行逻辑...
    }
}
```

### 9.2 原有执行器实现（保留兼容）
```java
@Component
public class ClaudeCodeExecutor implements AgentExecutor {

    @Override
    public ExecuteResult execute(Task task, TaskLog taskLog) {
        try {
            // 构建命令
            ProcessBuilder pb = new ProcessBuilder(
                "kiro-cli", "chat", task.getCommand()
            );

            // 重定向输出
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // 实时写入日志
                    taskLog.appendLog(line);
                }
            }

            int exitCode = process.waitFor();

            return ExecuteResult.builder()
                .success(exitCode == 0)
                .output(output.toString())
                .build();

        } catch (Exception e) {
            return ExecuteResult.failure(e.getMessage());
        }
    }

    @Override
    public boolean support(String agentType) {
        return "CLAUDE_CODE".equals(agentType);
    }
}
```

### 8.2 OpenClaw 执行器
```java
@Component
public class OpenClawExecutor implements AgentExecutor {

    @Override
    public ExecuteResult execute(Task task, TaskLog taskLog) {
        // 类似 Claude Code 的实现
        // 根据 OpenClaw 的调用方式调整命令
        ProcessBuilder pb = new ProcessBuilder(
            "openclaw", "run", task.getCommand()
        );
        // ... 执行逻辑
    }

    @Override
    public boolean support(String agentType) {
        return "OPENCLAW".equals(agentType);
    }
}
```

### 8.3 脚本执行器
```java
@Component
public class ScriptExecutor implements AgentExecutor {

    @Override
    public ExecuteResult execute(Task task, TaskLog taskLog) {
        // 支持 Shell/Python/Node.js 脚本
        String[] command = parseCommand(task.getCommand());
        ProcessBuilder pb = new ProcessBuilder(command);
        // ... 执行逻辑
    }

    @Override
    public boolean support(String agentType) {
        return "SCRIPT".equals(agentType);
    }
}
```

---

## 九、告警通知实现

### 9.1 邮件告警
```java
@Service
public class EmailAlarmService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendFailureAlarm(Task task, TaskLog log) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(task.getAlarmEmail());
        message.setSubject("【告警】任务执行失败 - " + task.getTaskName());
        message.setText(String.format(
            "任务名称: %s\n" +
            "执行时间: %s\n" +
            "失败原因: %s\n" +
            "日志详情: http://localhost:8080/log/%d",
            task.getTaskName(),
            log.getStartTime(),
            log.getErrorMsg(),
            log.getId()
        ));
        mailSender.send(message);
    }
}
```

### 9.2 企业微信/钉钉 Webhook
```java
@Service
public class WebhookAlarmService {

    @Autowired
    private RestTemplate restTemplate;

    public void sendAlarm(String webhookUrl, Task task, TaskLog log) {
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "text");
        body.put("text", Map.of(
            "content", String.format(
                "【任务告警】\n" +
                "任务: %s\n" +
                "状态: 失败\n" +
                "时间: %s",
                task.getTaskName(),
                log.getStartTime()
            )
        ));

        restTemplate.postForEntity(webhookUrl, body, String.class);
    }
}
```

---

## 十、项目结构

```
agent-scheduler/
├── src/main/java/com/agent/scheduler/
│   ├── AgentSchedulerApplication.java
│   ├── config/
│   │   ├── SchedulerConfig.java          # 调度器配置
│   │   ├── WebSocketConfig.java          # WebSocket配置
│   │   └── MyBatisConfig.java            # MyBatis 配置
│   ├── controller/
│   │   ├── ExecutorController.java       # 执行器管理
│   │   ├── TaskController.java           # 任务管理
│   │   ├── PromptTemplateController.java # Prompt模板
│   │   ├── LogController.java            # 日志查看
│   │   └── DashboardController.java      # 监控大盘
│   ├── service/
│   │   ├── ExecutorService.java          # 执行器服务
│   │   ├── PromptTemplateService.java    # Prompt服务
│   │   ├── TaskService.java              # 任务业务
│   │   ├── SchedulerService.java         # 调度服务
│   │   ├── LogService.java               # 日志服务
│   │   └── AlarmService.java             # 告警服务
│   ├── scheduler/
│   │   ├── DynamicScheduler.java         # 动态调度器
│   │   └── TaskExecutorService.java      # 任务执行
│   ├── mapper/
│   │   ├── ExecutorMapper.java
│   │   ├── PromptTemplateMapper.java
│   │   ├── TaskMapper.java
│   │   ├── TaskLogMapper.java
│   │   └── AlarmConfigMapper.java
│   ├── entity/
│   │   ├── Executor.java
│   │   ├── PromptTemplate.java
│   │   ├── Task.java
│   │   ├── TaskLog.java
│   │   └── AlarmConfig.java
│   └── common/
│       ├── Result.java                   # 统一返回
│       └── Constants.java                # 常量定义
├── src/main/resources/
│   ├── application.yml                   # 配置文件
│   ├── mapper/                           # MyBatis XML
│   ├── static/                           # 静态资源
│   │   ├── css/
│   │   └── js/
│   │       └── prompt-builder.js         # Prompt构建器
│   └── templates/                        # Thymeleaf 模板
│       ├── executor-list.html            # 执行器列表
│       ├── executor-form.html            # 执行器注册
│       ├── executor-detail.html          # 执行器详情（含实时输出）
│       ├── prompt-template-list.html     # Prompt模板库
│       ├── prompt-template-form.html     # Prompt模板表单
│       ├── task-list.html                # 任务列表
│       ├── task-form.html                # 任务表单（含Prompt引导）
│       ├── log-view.html                 # 日志查看
│       └── dashboard.html                # 监控大盘（含收藏执行器）
└── pom.xml
```

---

## 十一、配置文件示例

### 11.1 application.yml
```yaml
spring:
  application:
    name: agent-scheduler
  profiles:
    active: sqlite  # 默认使用 SQLite，可选 mysql
  datasource:
    url: jdbc:sqlite:./data/scheduler.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
  mail:
    host: smtp.example.com
    port: 587
    username: alert@example.com
    password: your_password

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.agent.scheduler.entity

# 调度配置
scheduler:
  pool-size: 10
  log-retention-days: 30
  archive-enabled: true
  archive-path: ./data/logs/archive

# 告警配置
alarm:
  enabled: true
  default-email: admin@example.com

---
# MySQL 配置（可选，团队部署使用）
spring:
  config:
    activate:
      on-profile: mysql
  datasource:
    url: jdbc:mysql://localhost:3306/agent_scheduler?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 11.2 Maven 依赖配置

**pom.xml 核心依赖**
```xml
<dependencies>
    <!-- SQLite 数据库驱动（默认） -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.1.0</version>
    </dependency>

    <!-- MySQL 驱动（可选） -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
</dependencies>
```

---

## 十二、快速开始

### 12.1 环境要求
- JDK 17+
- Maven 3.6+
- Claude Code / OpenClaw（根据需要）

### 12.2 部署步骤

**1. 下载并运行（零配置）**
```bash
# 下载 JAR 包
wget https://github.com/xxx/agent-scheduler/releases/latest/agent-scheduler.jar

# 直接运行（自动创建 SQLite 数据库）
java -jar agent-scheduler.jar

# 访问 Web 界面
open http://localhost:8080
```

**2. 使用 MySQL（可选，团队部署）**
```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE agent_scheduler DEFAULT CHARACTER SET utf8mb4;"

# 执行初始化脚本
mysql -u root -p agent_scheduler < init.sql

# 使用 MySQL 配置启动
java -jar agent-scheduler.jar --spring.profiles.active=mysql
```

**3. Windows 一键启动脚本**
```batch
@echo off
echo Starting Agent Scheduler...
java -jar agent-scheduler.jar
pause
```

**4. 编译运行**
```bash
mvn clean package
java -jar target/agent-scheduler-1.0.0.jar
```

**5. 访问系统**
```
http://localhost:8080
```

### 12.3 快速创建第一个执行器和任务

**步骤1：注册执行器**
1. 访问首页，点击"注册执行器"
2. 填写执行器信息：
   - 执行器名称：Claude Code本地
   - 执行器类型：Claude Code
   - 连接方式：CLI
   - 命令路径：kiro-cli
   - 健康检查命令：kiro-cli chat "ping"
3. 点击"测试连接"验证
4. 点击"保存"

**步骤2：创建任务**
1. 进入"任务管理"，点击"新建任务"
2. 填写任务信息：
   - 任务名称：每日代码审查
   - 选择执行器：Claude Code本地（确保状态为🟢在线）
   - 执行命令：kiro-cli chat "review recent changes"
   - Cron表达式：0 9 * * *（每天9点）
3. 点击"保存"
4. 点击"立即执行"测试

---

## 十三、核心优势

### 13.1 实时监控大盘
- **收藏执行器**：最多收藏3个常用执行器，置顶显示
- **流式输出**：WebSocket 实时推送 Agent 执行日志
- **状态可视化**：一眼看到执行器是否正在运行
- **快速跳转**：点击卡片直接进入执行器详情页

### 13.2 执行器详情页
- **实时日志流**：正在执行的任务实时显示输出
- **历史任务列表**：查看该执行器最近的执行记录
- **自动滚动**：支持暂停/恢复，方便查看历史输出
- **一键操作**：复制日志、下载完整记录

### 13.3 Prompt 工程引导
- **结构化配置**：角色定义、规则限制、Prompt 模板三段式设计
- **模板库**：预置常用场景模板，开箱即用
- **实时预览**：所见即所得，预览最终发送给 Agent 的命令
- **AI 辅助生成**：根据任务描述自动生成 Prompt

### 13.4 执行器注册中心
- **统一管理**：类似 Nacos，集中管理所有 Agent 执行器
- **健康监控**：主动下发测试消息，实时监控执行器状态
- **灵活配置**：支持 CLI/API/SSH 多种连接方式

### 13.5 简单易用
- **零配置启动**：开箱即用，无需复杂配置
- **直观界面**：Bootstrap 风格，清晰明了
- **快速上手**：5分钟注册执行器并创建任务

### 13.6 功能完整
- **Cron 调度**：支持标准 Cron 表达式
- **任务依赖**：支持前置任务依赖
- **超时控制**：防止任务无限执行
- **日志追踪**：完整的执行日志
- **告警通知**：多种告警方式

---

## 十四、后续扩展方向

### 14.1 短期（v1.0）
- [x] 基础任务调度
- [x] 日志管理
- [x] 邮件告警
- [ ] Web 界面优化
- [ ] 任务执行统计

### 14.2 中期（v2.0）
- [ ] 任务编排（DAG 工作流）
- [ ] 更多 Agent 支持
- [ ] 移动端告警（企业微信/钉钉）
- [ ] 任务执行可视化
- [ ] API 接口开放

### 14.3 长期（v3.0）
- [ ] 多用户支持
- [ ] 权限管理
- [ ] 任务市场（预设模板）
- [ ] AI 辅助配置
- [ ] 性能监控大盘

---

## 十五、技术选型说明

### 15.1 为什么选择 Spring Boot
- 快速开发，约定优于配置
- 生态完善，社区活跃
- 内置调度器，无需额外依赖

### 15.2 为什么选择 MyBatis
- 灵活的 SQL 控制
- 简单易学
- 适合中小型项目

### 15.3 为什么选择 Thymeleaf
- 服务端渲染，SEO 友好
- 无需前后端分离，降低复杂度
- 适合个人项目快速开发

---

## 十六、常见问题

**Q1: 如何注册新的执行器？**
1. 点击首页"注册执行器"
2. 填写执行器信息和连接配置
3. 配置健康检查命令
4. 点击"测试连接"验证
5. 保存后即可在任务中使用

**Q2: 执行器显示离线怎么办？**
- 点击"检测"按钮手动触发健康检查
- 检查执行器的命令路径是否正确
- 确认执行器进程是否正在运行
- 查看最后检测结果中的错误信息

**Q3: 如何添加新的连接方式？**
```java
// 在 TaskExecutorService 中添加新的执行方法
private ExecuteResult executeBySSH(Executor executor, Task task) {
    // 实现 SSH 连接逻辑
}
```

**Q4: 任务执行失败如何处理？**
- 系统会自动记录错误日志
- 如果配置了重试次数，会自动重试
- 发送告警通知到指定邮箱
- 检查执行器状态是否在线

**Q5: 如何备份执行器和任务配置？**
```bash
# 导出配置
mysqldump -u root -p agent_scheduler t_executor t_task > backup.sql

# 恢复配置
mysql -u root -p agent_scheduler < backup.sql
```

**Q6: 健康检查频率如何调整？**
```java
// 修改 ExecutorService 中的定时任务
@Scheduled(cron = "0 */5 * * * ?") // 改为你需要的频率
public void batchHealthCheck() {
    // ...
}
```

---

## 十七、总结

这是一个专为个人 Agent 管理设计的轻量级调度系统，核心特点：

✅ **实时监控大盘** - 收藏常用执行器，实时查看 Agent 执行状态和输出
✅ **流式日志推送** - WebSocket 实时传输，像看直播一样监控 Agent 工作
✅ **Prompt 工程引导** - 三段式结构化配置，帮助用户写出高质量 Prompt
✅ **执行器注册中心** - 类似 Nacos，统一管理所有 Agent
✅ **健康监控** - 主动检测执行器状态，实时可视化
✅ **简单易用** - 5分钟上手，无需学习成本

**核心创新点：**

1. **实时监控体验**
   - 监控大盘显示收藏的3个执行器
   - 正在执行的任务实时显示流式输出
   - 点击执行器卡片跳转到详情页
   - 执行器详情页展示当前任务和历史记录

2. **Prompt 工程可视化**
   - 角色定义、规则限制、Prompt 模板分离
   - 实时预览最终命令
   - 内置提示和最佳实践引导

3. **执行器注册中心**
   - 执行器与任务解耦，一个执行器可被多个任务复用
   - 无需 Agent 主动心跳，调度中心主动下发检测命令
   - 首页直观展示所有执行器状态

4. **AI Agent 专属优化**
   - 针对 AI Agent 特性设计的 Prompt 配置界面
   - 明确角色边界，避免 Agent 越权操作
   - 实时查看 Agent 思考和执行过程

适合场景：
- 个人开发者管理多个 AI Agent（Claude Code、OpenClaw 等）
- 需要实时监控 Agent 执行过程
- 定时执行代码审查、文档生成、测试运行等任务
- 需要精细控制 Agent 行为的自动化场景

下一步：开始编码实现！