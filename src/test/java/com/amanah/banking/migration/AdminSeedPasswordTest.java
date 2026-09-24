package com.amanah.banking.migration;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the seeded default admin credentials documented in README.md.
 *
 * <p>Flyway applies migrations in version order, so the last BCrypt hash found in a
 * migration that touches the {@code admin} user is the value that ends up in
 * {@code users.password_hash}. If that hash does not verify against the documented
 * password the default admin account is unusable.</p>
 *
 * <p>This is a regression test for a real defect: V5 seeded a placeholder hash that
 * matched no candidate password (login as {@code admin} always returned 401); V6
 * replaces it.</p>
 */
class AdminSeedPasswordTest {

    /** Must stay in sync with README.md ("Default admin account"). */
    private static final String DOCUMENTED_ADMIN_PASSWORD = "Admin@1234";

    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    @Test
    void effectiveSeededAdminHash_matchesDocumentedPassword() throws IOException {
        String hash = effectiveAdminPasswordHash();

        assertNotNull(hash, "no BCrypt admin password_hash found in db/migration");
        assertTrue(BCrypt.checkpw(DOCUMENTED_ADMIN_PASSWORD, hash),
            "seeded admin hash does not verify against '" + DOCUMENTED_ADMIN_PASSWORD + "': " + hash);
    }

    @Test
    void effectiveSeededAdminHash_usesBcryptCost12() throws IOException {
        String hash = effectiveAdminPasswordHash();

        assertNotNull(hash, "no BCrypt admin password_hash found in db/migration");
        assertEquals(60, hash.length(), "BCrypt hash must be 60 chars: " + hash);
        assertTrue(hash.startsWith("$2a$12$"),
            "must match BCryptPasswordEncoder(12) used by SecurityConfig: " + hash);
    }

    @Test
    void readmeDocumentsTheSeededAdminPassword() throws IOException {
        String readme = read("README.md");

        assertTrue(readme.contains(DOCUMENTED_ADMIN_PASSWORD),
            "README.md must document the seeded admin password");
        assertTrue(readme.contains("admin"), "README.md must document the admin username");
    }

    /** Last admin hash in migration order == the value Flyway ends up applying. */
    private String effectiveAdminPasswordHash() throws IOException {
        String found = null;
        for (Path file : migrationFilesInOrder()) {
            String sql = read(file.toString());
            if (!sql.contains("'admin'")) {
                continue;
            }
            Matcher m = BCRYPT.matcher(sql);
            while (m.find()) {
                found = m.group();
            }
        }
        return found;
    }

    private List<Path> migrationFilesInOrder() throws IOException {
        Path dir = Paths.get("src", "main", "resources", "db", "migration");
        assertTrue(Files.isDirectory(dir), "migration directory not found: " + dir.toAbsolutePath().normalize());
        try (Stream<Path> files = Files.list(dir)) {
            return files
                .filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                .sorted(Comparator.comparingInt(AdminSeedPasswordTest::versionOf))
                .collect(Collectors.toList());
        }
    }

    private static int versionOf(Path file) {
        Matcher m = Pattern.compile("^V(\\d+)__").matcher(file.getFileName().toString());
        assertTrue(m.find(), "unexpected migration file name: " + file);
        return Integer.parseInt(m.group(1));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
