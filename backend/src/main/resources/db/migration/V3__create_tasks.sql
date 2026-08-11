CREATE TABLE task (
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT REFERENCES project (id),
    assignee_id      BIGINT REFERENCES employee (id),
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'TODO',
    priority         VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    estimated_hours  NUMERIC(6, 2),
    start_date       DATE,
    due_date         DATE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX ix_task_project_id ON task (project_id);
CREATE INDEX ix_task_assignee_id ON task (assignee_id);
CREATE INDEX ix_task_status ON task (status);
CREATE INDEX ix_task_due_date ON task (due_date);
