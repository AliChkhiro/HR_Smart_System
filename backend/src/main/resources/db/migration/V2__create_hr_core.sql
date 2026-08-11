CREATE TABLE department (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    manager_id  BIGINT REFERENCES app_user (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_department_name_active ON department (name) WHERE deleted_at IS NULL;

CREATE TABLE employee (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES app_user (id),
    department_id BIGINT REFERENCES department (id),
    job_title     VARCHAR(100) NOT NULL,
    hire_date     DATE         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_employee_user_active ON employee (user_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_employee_department_id ON employee (department_id);

CREATE TABLE skill (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    category   VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_skill_name_active ON skill (name) WHERE deleted_at IS NULL;

CREATE TABLE employee_skill (
    id          BIGSERIAL PRIMARY KEY,
    employee_id BIGINT      NOT NULL REFERENCES employee (id),
    skill_id    BIGINT      NOT NULL REFERENCES skill (id),
    level       INTEGER     NOT NULL DEFAULT 1 CHECK (level BETWEEN 1 AND 5),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_employee_skill UNIQUE (employee_id, skill_id)
);

CREATE INDEX ix_employee_skill_skill_id ON employee_skill (skill_id);

CREATE TABLE project (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(30)  NOT NULL DEFAULT 'PLANNED',
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_project_name_active ON project (name) WHERE deleted_at IS NULL;

CREATE TABLE project_member (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT      NOT NULL REFERENCES project (id),
    employee_id BIGINT      NOT NULL REFERENCES employee (id),
    role        VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_member UNIQUE (project_id, employee_id)
);

CREATE INDEX ix_project_member_employee_id ON project_member (employee_id);
