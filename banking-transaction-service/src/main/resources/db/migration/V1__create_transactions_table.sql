CREATE TABLE transactions (
    transaction_id  VARCHAR(255)   PRIMARY KEY,
    account_id      VARCHAR(255)   NOT NULL,
    type            VARCHAR(50)    NOT NULL,
    amount          NUMERIC(19,4)  NOT NULL,
    balance_after   NUMERIC(19,4)  NOT NULL,
    description     VARCHAR(500)   NOT NULL,
    timestamp       TIMESTAMP      NOT NULL
);

CREATE INDEX idx_account_id ON transactions(account_id);
CREATE INDEX idx_timestamp  ON transactions(timestamp);