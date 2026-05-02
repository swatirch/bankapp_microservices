CREATE TABLE accounts (
    account_id      VARCHAR(255) PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    account_number  VARCHAR(255) NOT NULL UNIQUE,
    owner_name      VARCHAR(255) NOT NULL,
    account_type    VARCHAR(50)  NOT NULL,
    balance         NUMERIC(19,4) NOT NULL,
    account_status  VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    owner_id        VARCHAR(255)
);