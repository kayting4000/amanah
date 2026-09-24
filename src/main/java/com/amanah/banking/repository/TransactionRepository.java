package com.amanah.banking.repository;

import com.amanah.banking.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Transaction> rowMapper = (rs, rowNum) -> {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setReferenceNumber(rs.getString("reference_number"));
        t.setAccountId(rs.getLong("account_id"));
        long related = rs.getLong("related_account_id");
        if (!rs.wasNull()) t.setRelatedAccountId(related);
        t.setTransactionType(Transaction.TransactionType.valueOf(rs.getString("transaction_type")));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setDescription(rs.getString("description"));
        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return t;
    };

    public Long save(Transaction transaction) {
        String sql = """
            INSERT INTO transactions
                (reference_number, account_id, related_account_id, transaction_type,
                 amount, balance_before, balance_after, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, transaction.getReferenceNumber());
            ps.setLong(2, transaction.getAccountId());
            if (transaction.getRelatedAccountId() != null) {
                ps.setLong(3, transaction.getRelatedAccountId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, transaction.getTransactionType().name());
            ps.setBigDecimal(5, transaction.getAmount());
            ps.setBigDecimal(6, transaction.getBalanceBefore());
            ps.setBigDecimal(7, transaction.getBalanceAfter());
            ps.setString(8, transaction.getDescription());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Transaction> findByAccountId(Long accountId) {
        String sql = """
            SELECT * FROM transactions
            WHERE account_id = ?
            ORDER BY created_at DESC
            """;
        return jdbc.query(sql, rowMapper, accountId);
    }

    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        return jdbc.query(sql, rowMapper, id).stream().findFirst();
    }
}
