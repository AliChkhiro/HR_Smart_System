CREATE TABLE task_skill (
    task_id  BIGINT NOT NULL REFERENCES task (id),
    skill_id BIGINT NOT NULL REFERENCES skill (id),
    PRIMARY KEY (task_id, skill_id)
);

CREATE INDEX ix_task_skill_skill_id ON task_skill (skill_id);