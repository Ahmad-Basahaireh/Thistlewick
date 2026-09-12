-- =====================================================
-- Thistlewick — Database schema (H2)
-- =====================================================
-- This file is executed at startup by SchemaInitializer.
-- Statements are idempotent (IF NOT EXISTS) to allow re-runs.

-- -----------------------------------------------------
-- Users
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name   VARCHAR(100)  NOT NULL,
    email  VARCHAR(254)  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- -----------------------------------------------------
-- Tasks
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id     BIGINT        NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(2000) NOT NULL DEFAULT '',
    due_date     TIMESTAMP     NOT NULL,
    priority     VARCHAR(10)   NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW','MEDIUM','HIGH')),
    CONSTRAINT chk_tasks_status   CHECK (status IN ('TODO','IN_PROGRESS','DONE'))
);

CREATE INDEX IF NOT EXISTS idx_tasks_owner    ON tasks(owner_id);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);

-- -----------------------------------------------------
-- Task tags (1 task → many tags)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS task_tags (
    task_id  BIGINT       NOT NULL,
    tag      VARCHAR(50)  NOT NULL,
    PRIMARY KEY (task_id, tag),
    CONSTRAINT fk_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- -----------------------------------------------------
-- Reminders
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS reminders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT       NOT NULL,
    trigger_time  TIMESTAMP    NOT NULL,
    channel       VARCHAR(10)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    fired_at      TIMESTAMP,
    CONSTRAINT fk_reminders_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT chk_reminders_channel CHECK (channel IN ('EMAIL','SMS','PUSH')),
    CONSTRAINT chk_reminders_status  CHECK (status IN ('PENDING','FIRED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_reminders_task       ON reminders(task_id);
CREATE INDEX IF NOT EXISTS idx_reminders_trigger    ON reminders(trigger_time);
CREATE INDEX IF NOT EXISTS idx_reminders_status     ON reminders(status);