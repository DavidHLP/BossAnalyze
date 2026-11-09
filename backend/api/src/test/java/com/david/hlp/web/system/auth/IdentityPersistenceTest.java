package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.mapper.TokenMapper;
import com.david.hlp.web.system.auth.token.Token;
import com.david.hlp.web.system.auth.token.TokenType;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class IdentityPersistenceTest {
    String url() { return "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"; }

    void table(Connection connection, String name) throws Exception {
        String schema = Files.readString(Path.of("../../database/system.sql"));
        var matcher = Pattern.compile("CREATE TABLE `" + name + "` \\((.*?)\\) ENGINE=", Pattern.DOTALL).matcher(schema);
        assertTrue(matcher.find());
        // H2 uses the production columns/indexes; omit only the unrelated user FK.
        // H2 cannot model MySQL ASCII/collation; real MySQL migration rehearsal is separate.
        String body = matcher.group(1).replace(" CHARACTER SET ascii COLLATE ascii_bin", "").replaceAll(",\\s*CONSTRAINT `token_ibfk_1`[^\\n]*", "");
        connection.createStatement().execute("CREATE TABLE `" + name + "` (" + body + ")");
    }

    @Test void productionTokenStatementsExecuteAndLogoutInvalidatesReplay() throws Exception {
        String url = url();
        try (var connection = DriverManager.getConnection(url)) { table(connection,"token"); }
        var config = new Configuration(new Environment("isolated", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.h2.Driver",url,null)));
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(TokenMapper.class);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession(true)) {
            var mapper = session.getMapper(TokenMapper.class);
            var jwt = JwtIdentityTest.service();
            var user = org.springframework.security.core.userdetails.User.withUsername("synthetic@example.test")
                    .password("unused").authorities("read").build();
            var token = Token.builder().userId(1L).token(jwt.generateToken(user)).tokenType(TokenType.ACCESS).build();
            mapper.save(token);
            assertTrue(mapper.checkTokenValid(token.getToken()));
            assertNotNull(mapper.getByToken(token.getToken()).getCreateTime());
            assertEquals(1,mapper.listValidTokensByUser(1L).size());
            var another = Token.builder().userId(1L).token(jwt.generateToken(user)).tokenType(TokenType.ACCESS).build();
            mapper.save(another);
            assertEquals(2, mapper.listValidTokensByUser(1L).size());
            var request = new org.springframework.mock.web.MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token.getToken());
            var logout = new LogoutService(mapper);
            logout.logout(request, new org.springframework.mock.web.MockHttpServletResponse(), null);
            logout.logout(request, new org.springframework.mock.web.MockHttpServletResponse(), null);
            mapper.revoke("unknown");
            assertFalse(mapper.checkTokenValid(token.getToken()));
            assertTrue(mapper.checkTokenValid(another.getToken()), "logout must preserve the other session");
            assertEquals(1, mapper.listValidTokensByUser(1L).size());
            assertNotNull(mapper.getByToken(token.getToken()).getCreateTime(),"revocation preserves record/time");
            another.setExpired(true); another.setRevoked(true);
            mapper.updateBatch(List.of(another));
            assertFalse(mapper.checkTokenValid(another.getToken()));
        }
    }

    @Test void maximumStoredIdentityFitsAndTokenUniquenessPreservesSeparateSessions() throws Exception {
        String url = url();
        var jwt = JwtIdentityTest.service();
        try (var connection = DriverManager.getConnection(url)) {
            table(connection, "token");
            var insert = connection.prepareStatement("INSERT INTO token(user_id, token, token_type) VALUES(1, ?, 'ACCESS')");
            // user.email is VARCHAR(255), with no DTO length/character validator.
            for (String identity : List.of("a".repeat(242) + "@example.test", "😀".repeat(255), "\u0001".repeat(255))) {
                var user = org.springframework.security.core.userdetails.User.withUsername(identity)
                        .password("unused").authorities("read").build();
                for (String token : List.of(jwt.generateToken(user), jwt.generateToken(java.util.Map.of(), user), jwt.generateRefreshToken(user))) {
                    assertTrue(token.length() > 255);
                    assertTrue(token.length() <= 2560, "token length: " + token.length());
                    assertEquals(identity, jwt.extractUsername(token));
                    assertTrue(token.chars().allMatch(c -> c < 128));
                    insert.setString(1, token); insert.executeUpdate();
                    var select = connection.prepareStatement("SELECT token FROM token WHERE token = ?");
                    select.setString(1, token);
                    try (var rows = select.executeQuery()) { assertTrue(rows.next()); assertEquals(token, rows.getString(1)); }
                    assertEquals("23505", assertThrows(SQLException.class, insert::executeUpdate, "full token must be unique").getSQLState());
                }
            }
            insert.setString(1, "CaseSensitive"); insert.executeUpdate();
            insert.setString(1, "casesensitive"); insert.executeUpdate();
            connection.createStatement().executeUpdate("UPDATE token SET revoked = TRUE WHERE token = 'CaseSensitive'");
            try (var rows = connection.createStatement().executeQuery("SELECT revoked FROM token WHERE token = 'casesensitive'")) {
                assertTrue(rows.next()); assertFalse(rows.getBoolean(1));
            }
        }
    }

    @Test void databaseConstraintRejectsConcurrentDuplicateNames() throws Exception {
        String url = url();
        try (var connection = DriverManager.getConnection(url)) { table(connection,"role"); }
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> insert = () -> {
            start.await();
            try (var connection = DriverManager.getConnection(url)) {
                connection.createStatement().executeUpdate("INSERT INTO role(role_name) VALUES ('synthetic-role')");
                return true;
            } catch (SQLException ex) { assertEquals("23505",ex.getSQLState()); return false; }
        };
        try {
            var a = executor.submit(insert); var b = executor.submit(insert); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            try (var connection = DriverManager.getConnection(url)) {
                var rows = connection.createStatement().executeQuery("SELECT COUNT(*) FROM role"); rows.next();
                assertEquals(1,rows.getInt(1));
            }
        } finally { executor.shutdownNow(); }
    }
}
