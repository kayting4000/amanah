CREATE TABLE transactions (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    reference_number   VARCHAR(20)     NOT NULL,
    account_id         BIGINT          NOT NULL,
    related_account_id BIGINT          DEFAULT NULL,
    transaction_type   ENUM('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT') NOT NULL,
    amount             DECIMAL(19,4)   NOT NULL,
    balance_before     DECIMAL(19,4)   NOT NULL,
    balance_after      DECIMAL(19,4)   NOT NULL,
    description        VARCHAR(255)    DEFAULT NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_transactions_ref (reference_number),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transactions_related FOREIGN KEY (related_account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
