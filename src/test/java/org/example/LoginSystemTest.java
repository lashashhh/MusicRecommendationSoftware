package org.example;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Field;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class LoginSystemTest {

    private Connection createConnection(String dbName) throws SQLException {
        String url = "jdbc:h2:mem:" + dbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        return DriverManager.getConnection(url, "sa", "");
    }

    private void initUserSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username      VARCHAR(255) UNIQUE,
                    password_hash VARCHAR(255)
                );
            """);
        }
    }

    private void setTestConnection(loginSystem ls, Connection conn) throws Exception {
        Field f = loginSystem.class.getDeclaredField("testConnection");
        f.setAccessible(true);
        f.set(ls, conn);
    }

    @Test
    void successfulLogin_setsCurrentUserIdAndReturnsTrue() throws Exception {
        try (Connection conn = createConnection("login_success")) {
            initUserSchema(conn);

            String username = "alice";
            String rawPassword = "secret123";
            String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        loginSystem.setCurrentUserId(0);

                        loginSystem ls = new loginSystem();
                        setTestConnection(ls, conn);

                        boolean ok = ls.tryLogin(username, rawPassword);
                        assertTrue(ok, "Login with correct password should succeed");
                        assertEquals(id, loginSystem.getCurrentUserId(),
                                "currentUserId should be set to the logged-in user's id");
                    } else {
                        fail("Failed to insert test user");
                    }
                }
            }
        }
    }

    @Test
    void wrongPassword_returnsFalse_andDoesNotChangeCurrentUserId() throws Exception {
        try (Connection conn = createConnection("login_wrong_pw")) {
            initUserSchema(conn);

            String username = "bob";
            String rawPassword = "correct_pw";
            String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            int initialId = 42;
            loginSystem.setCurrentUserId(initialId);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.executeUpdate();
            }

            loginSystem ls = new loginSystem();
            setTestConnection(ls, conn);

            boolean ok = ls.tryLogin(username, "wrong_pw");
            assertFalse(ok, "Login with wrong password should fail");
            assertEquals(initialId, loginSystem.getCurrentUserId(),
                    "currentUserId should not change on failed login");
        }
    }

    @Test
    void unknownUser_returnsFalse() throws Exception {
        try (Connection conn = createConnection("login_unknown")) {
            initUserSchema(conn);

            loginSystem.setCurrentUserId(0);
            loginSystem ls = new loginSystem();
            setTestConnection(ls, conn);

            boolean ok = ls.tryLogin("nosuchuser", "whatever");
            assertFalse(ok, "Login with unknown username should fail");
            assertEquals(0, loginSystem.getCurrentUserId(),
                    "currentUserId should remain 0 when login fails");
        }
    }

    @Test
    void signupCreatesUserWithHashedPassword() throws Exception {
        // 1) Init schema in this DB
        final String dbName = "signup_hash";
        try (Connection setupConn = createConnection(dbName)) {
            initUserSchema(setupConn);
        }

        String username = "newuser";
        String rawPassword = "mySecret123";

        loginSystem.setCurrentUserId(0);
        loginSystem ls = new loginSystem();
        setTestConnection(ls, createConnection(dbName));

        boolean ok = ls.trySignup(username, rawPassword);
        assertTrue(ok, "Signup with a fresh username should succeed");

        // 3) Verify using a separate connection to the same in-memory DB
        try (Connection verifyConn = createConnection(dbName);
             PreparedStatement ps = verifyConn.prepareStatement(
                     "SELECT id, username, password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "User row should exist after signup");
                String storedHash = rs.getString("password_hash");
                assertNotNull(storedHash, "Password hash should not be null");
                assertNotEquals(rawPassword, storedHash, "Password should not be stored in plain text");
                assertTrue(BCrypt.checkpw(rawPassword, storedHash),
                        "Stored hash should match the original password");
            }
        }
    }

    @Test
    void signupFailsForDuplicateUsername() throws Exception {
        final String dbName = "signup_duplicate";

        try (Connection setupConn = createConnection(dbName)) {
            initUserSchema(setupConn);
        }

        String username = "duplicateUser";

        {
            loginSystem ls1 = new loginSystem();
            setTestConnection(ls1, createConnection(dbName));
            boolean first = ls1.trySignup(username, "pw1");
            assertTrue(first, "First signup should succeed");
        }

        {
            loginSystem ls2 = new loginSystem();
            setTestConnection(ls2, createConnection(dbName)); // new conn, same DB
            boolean second = ls2.trySignup(username, "pw2");
            assertFalse(second, "Signup must fail for duplicate username");
        }

        try (Connection verifyConn = createConnection(dbName);
             PreparedStatement ps = verifyConn.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "There should still be only one user row");
            }
        }
    }


}
