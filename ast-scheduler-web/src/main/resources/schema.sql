-- Agent 调度中心 SQLite 数据库初始化脚本

-- 1. 执行器注册表
CREATE TABLE IF NOT EXISTS t_executor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    executor_name VARCHAR(100) NOT NULL,
    executor_type VARCHAR(50) NOT NULL,
    executor_desc VARCHAR(500),
    connection_type VARCHAR(50) NOT NULL,
    connection_config TEXT,
    health_check_command TEXT,
    status INTEGER DEFAULT 0,
    is_favorite INTEGER DEFAULT 0,
    favorite_order INTEGER DEFAULT 0,
    last_check_time DATETIME,
    last_check_result TEXT,
    group_name VARCHAR(50) DEFAULT 'default',
    sort_order INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
)$$

CREATE INDEX IF NOT EXISTS idx_executor_type ON t_executor(executor_type)$$
CREATE INDEX IF NOT EXISTS idx_executor_status ON t_executor(status)$$
CREATE INDEX IF NOT EXISTS idx_executor_favorite ON t_executor(is_favorite)$$
CREATE INDEX IF NOT EXISTS idx_executor_group ON t_executor(group_name)$$

CREATE TRIGGER IF NOT EXISTS update_executor_timestamp
AFTER UPDATE ON t_executor
BEGIN
    UPDATE t_executor SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END$$

-- 2. 任务配置表
CREATE TABLE IF NOT EXISTS t_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_name VARCHAR(100) NOT NULL,
    task_desc VARCHAR(500),
    executor_id INTEGER NOT NULL,
    prompt_template TEXT,
    agent_role VARCHAR(200),
    agent_rules TEXT,
    command TEXT NOT NULL,
    cron_expression VARCHAR(100),
    timeout_seconds INTEGER DEFAULT 3600,
    retry_count INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    depend_task_id INTEGER,
    alarm_email VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
)$$

CREATE INDEX IF NOT EXISTS idx_task_status ON t_task(status)$$
CREATE INDEX IF NOT EXISTS idx_task_executor_id ON t_task(executor_id)$$

CREATE TRIGGER IF NOT EXISTS update_task_timestamp
AFTER UPDATE ON t_task
BEGIN
    UPDATE t_task SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END$$

-- 3. 任务执行日志表
CREATE TABLE IF NOT EXISTS t_task_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    trigger_type INTEGER,
    start_time DATETIME,
    end_time DATETIME,
    duration_ms INTEGER,
    status INTEGER,
    log_content TEXT,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
)$$

CREATE INDEX IF NOT EXISTS idx_log_task_id ON t_task_log(task_id)$$
CREATE INDEX IF NOT EXISTS idx_log_start_time ON t_task_log(start_time)$$
CREATE INDEX IF NOT EXISTS idx_log_status ON t_task_log(status)$$

-- 4. Prompt模板表
CREATE TABLE IF NOT EXISTS t_prompt_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_name VARCHAR(100) NOT NULL,
    template_desc VARCHAR(500),
    agent_type VARCHAR(50),
    role_definition TEXT,
    rules_template TEXT,
    prompt_template TEXT,
    variables VARCHAR(500),
    is_system INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
)$$

CREATE INDEX IF NOT EXISTS idx_template_agent_type ON t_prompt_template(agent_type)$$

CREATE TRIGGER IF NOT EXISTS update_prompt_template_timestamp
AFTER UPDATE ON t_prompt_template
BEGIN
    UPDATE t_prompt_template SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END$$

-- 5. 告警配置表
CREATE TABLE IF NOT EXISTS t_alarm_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_name VARCHAR(100) NOT NULL,
    alarm_type VARCHAR(50) NOT NULL,
    config_json TEXT,
    is_default INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
)$$

CREATE TRIGGER IF NOT EXISTS update_alarm_config_timestamp
AFTER UPDATE ON t_alarm_config
BEGIN
    UPDATE t_alarm_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END$$

-- 6. 用户配置表
CREATE TABLE IF NOT EXISTS t_user_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(100) NOT NULL,
    language VARCHAR(10) DEFAULT 'zh',
    theme VARCHAR(20) DEFAULT 'light',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
)$$

CREATE TRIGGER IF NOT EXISTS update_user_config_timestamp
AFTER UPDATE ON t_user_config
BEGIN
    UPDATE t_user_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END$$
