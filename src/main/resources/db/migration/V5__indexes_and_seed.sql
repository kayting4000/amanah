-- Indexes for performance
CREATE INDEX idx_accounts_customer ON accounts (customer_id);
CREATE INDEX idx_transactions_account ON transactions (account_id);
CREATE INDEX idx_transactions_created ON transactions (created_at);

-- Seed default admin user (password: Admin@1234)
INSERT INTO users (username, email, password_hash, role, status)
VALUES (
    'admin',
    'admin@amanah.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'ADMIN',
    'ACTIVE'
);
