package com.example;

import com.example.db.Tables;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void generatedSqlUsesLowercasePostgresIdentifiers() {
        var sql = DSL.using(SQLDialect.POSTGRES)
            .select(Tables.USERS.ID, Tables.USERS.EMAIL)
            .from(Tables.USERS)
            .where(Tables.USERS.ACTIVE.isTrue())
            .getSQL();

        assertAll(
            () -> assertTrue(sql.contains("\"users\""),  "table name should be lowercase"),
            () -> assertTrue(sql.contains("\"id\""),     "column id should be lowercase"),
            () -> assertTrue(sql.contains("\"email\""),  "column email should be lowercase"),
            () -> assertTrue(sql.contains("\"active\""), "column active should be lowercase")
        );
    }
}
