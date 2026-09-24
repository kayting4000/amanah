package com.amanah.banking.repository;

import com.amanah.banking.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Account> rowMapper = (rs, rowNum) -> {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setCustomerId(rs.getLong("customer_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountType(Account.AccountType.valueOf(rs.getString("account_type")));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(Account.AccountStatus.valueOf(rs.getString("status")));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return a;
    };

    public Long save(Account account) {
        String sql = """
            INSERT INTO accounts (customer_id, account_number, account_type, balance, status)
            VALUES (?, ?, ?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, account.getCustomerId());
            ps.setString(2, account.getAccountNumber());
            ps.setString(3, account.getAccountType().name());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getStatus().name());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<Account> findById(Long id) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        return jdbc.query(sql, rowMapper, id).stream().findFirst();
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        return jdbc.query(sql, rowMapper, accountNumber).stream().findFirst();
    }

    public List<Account> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM accounts WHERE customer_id = ?";
        return jdbc.query(sql, rowMapper, customerId);
    }

    /**
     * Atomic deposit: adds amount unconditionally.
     * Returns updated row count.
     */
    public int deposit(Long accountId, BigDecimal amount) {
        String sql = """
            UPDATE accounts
            SET balance = balance + ?
            WHERE id = ? AND status = 'ACTIVE'
            """;
        return jdbc.update(sql, amount, accountId);
    }

    /**
     * Atomic withdrawal: only succeeds if balance >= amount.
     * Returns updated row count (0 = insufficient balance or not active).
     */
    public int withdraw(Long accountId, BigDecimal amount) {
        String sql = """
            UPDATE accounts
            SET balance = balance - ?
            WHERE id = ? AND status = 'ACTIVE' AND balance >= ?
            """;
        return jdbc.update(sql, amount, accountId, amount);
    }

    public void updateStatus(Long accountId, Account.AccountStatus status) {
        String sql = "UPDATE accounts SET status = ? WHERE id = ?";
        jdbc.update(sql, status.name(), accountId);
    }

    public boolean existsByAccountNumber(String accountNumber) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE account_number = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, accountNumber);
        return count != null && count > 0;
    }
}
