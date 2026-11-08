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
        String body = matcher.group(1).replaceAll(",\\s*CONSTRAINT `token_ibfk_1`[^\\n]*", "");
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
            var token = Token.builder().userId(1L).token("synthetic-token").tokenType(TokenType.ACCESS).build();
            mapper.save(token);
            assertTrue(mapper.checkTokenValid(token.getToken()));
            assertNotNull(mapper.getByToken(token.getToken()).getCreateTime());
            assertEquals(1,mapper.listValidTokensByUser(1L).size());
            mapper.revoke(token.getToken()); mapper.revoke(token.getToken()); mapper.revoke("unknown");
            assertFalse(mapper.checkTokenValid(token.getToken()));
            assertTrue(mapper.listValidTokensByUser(1L).isEmpty());
            assertNotNull(mapper.getByToken(token.getToken()).getCreateTime(),"revocation preserves record/time");
            var another = Token.builder().userId(1L).token("second-synthetic-token").tokenType(TokenType.ACCESS).build();
            mapper.save(another);
            another.setExpired(true); another.setRevoked(true);
            mapper.updateBatch(List.of(another));
            assertFalse(mapper.checkTokenValid(another.getToken()));
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
