CREATE TABLE erasure_request (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES app_user (id),
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason             VARCHAR(500),
    requested_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at         TIMESTAMPTZ,
    decided_by_user_id BIGINT       REFERENCES app_user (id)
);

CREATE INDEX ix_erasure_request_user_id ON erasure_request (user_id);
CREATE UNIQUE INDEX ux_erasure_request_pending ON erasure_request (user_id) WHERE status = 'PENDING';
