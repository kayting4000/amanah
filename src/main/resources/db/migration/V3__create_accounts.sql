CREATE TABLE accounts (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    customer_id    BIGINT          NOT NULL,
    account_number VARCHAR(12)     NOT NULL,
    account_type   ENUM('SAVINGS','WADIAH') NOT NULL DEFAULT 'SAVINGS',
    balance        DECIMAL(19,4)   NOT NULL DEFAULT 0.0000,
    status         ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_accounts_number (account_number),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
