-- Journal d'audit : table append-only, volontairement SANS clé étrangère vers
-- app_user. Une contrainte FK empêcherait de tracer un acteur créé dans la même
-- transaction (compte pas encore validé) et bloquerait toute suppression de compte.
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   BIGINT,
    actor_email     VARCHAR(255),
    action          VARCHAR(60)   NOT NULL,
    entity_type     VARCHAR(60)   NOT NULL,
    entity_id       VARCHAR(64),
    detail          VARCHAR(1000),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_log_actor_user_id ON audit_log (actor_user_id);
CREATE INDEX ix_audit_log_action ON audit_log (action);
CREATE INDEX ix_audit_log_entity_type_entity_id ON audit_log (entity_type, entity_id);
CREATE INDEX ix_audit_log_created_at ON audit_log (created_at);
