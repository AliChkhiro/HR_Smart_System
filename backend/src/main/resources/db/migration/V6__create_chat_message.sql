CREATE TABLE chat_message (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES app_user (id),
    role       VARCHAR(20)   NOT NULL,
    content    VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_chat_message_user_id ON chat_message (user_id);
