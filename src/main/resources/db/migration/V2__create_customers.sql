CREATE TABLE customers (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    first_name  VARCHAR(50)     NOT NULL,
    middle_name VARCHAR(50)     DEFAULT NULL,
    last_name   VARCHAR(50)     NOT NULL,
    phone       VARCHAR(20)     NOT NULL,
    address     VARCHAR(255)    DEFAULT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_customers_user_id (user_id),
    UNIQUE KEY uq_customers_phone (phone),
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
