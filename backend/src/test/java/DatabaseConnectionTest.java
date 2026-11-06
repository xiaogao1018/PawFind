
package com.pawfind.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() {
        String databaseName =
                jdbcTemplate.queryForObject(
                        "SELECT DATABASE()",
                        String.class
                );

        System.out.println("当前连接数据库：" + databaseName);

        assertEquals("pawfind", databaseName);
    }
}
