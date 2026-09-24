package com.amanah.banking.repository;

import com.amanah.banking.model.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Customer> rowMapper = (rs, rowNum) -> {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setFirstName(rs.getString("first_name"));
        c.setMiddleName(rs.getString("middle_name"));
        c.setLastName(rs.getString("last_name"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        c.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return c;
    };

    public Long save(Customer customer) {
        String sql = """
            INSERT INTO customers (user_id, first_name, middle_name, last_name, phone, address)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customer.getUserId());
            ps.setString(2, customer.getFirstName());
            ps.setString(3, customer.getMiddleName());
            ps.setString(4, customer.getLastName());
            ps.setString(5, customer.getPhone());
            ps.setString(6, customer.getAddress());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<Customer> findByUserId(Long userId) {
        String sql = "SELECT * FROM customers WHERE user_id = ?";
        return jdbc.query(sql, rowMapper, userId).stream().findFirst();
    }

    public Optional<Customer> findById(Long id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        return jdbc.query(sql, rowMapper, id).stream().findFirst();
    }

    public void update(Customer customer) {
        String sql = """
            UPDATE customers
            SET first_name = ?, middle_name = ?, last_name = ?, phone = ?, address = ?
            WHERE id = ?
            """;
        jdbc.update(sql,
            customer.getFirstName(), customer.getMiddleName(),
            customer.getLastName(), customer.getPhone(),
            customer.getAddress(), customer.getId());
    }
}
