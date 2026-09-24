ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_balance_nonnegative CHECK (balance >= 0);

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    ADD CONSTRAINT chk_transactions_balances_nonnegative CHECK (balance_before >= 0 AND balance_after >= 0);