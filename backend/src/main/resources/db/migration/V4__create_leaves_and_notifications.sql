CREATE TABLE leave_request (
    id             BIGSERIAL PRIMARY KEY,
    employee_id    BIGINT       NOT NULL REFERENCES employee (id),
    type           VARCHAR(30)  NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason         VARCHAR(500),
    reviewer_id    BIGINT REFERENCES employee (id),
    review_date    TIMESTAMPTZ,
    review_comment VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX ix_leave_employee_id ON leave_request (employee_id);
CREATE INDEX ix_leave_status ON leave_request (status);
CREATE INDEX ix_leave_start_date ON leave_request (start_date);

CREATE TABLE notification (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id),
    type       VARCHAR(30)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX ix_notification_user_id ON notification (user_id);
CREATE INDEX ix_notification_user_read ON notification (user_id, is_read);
