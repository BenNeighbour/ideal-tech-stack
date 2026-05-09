package com.example;

import com.example.db.Tables;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // No DB connection — JOOQ renders the query as a SQL string using the Postgres dialect.
        var sql = DSL.using(SQLDialect.POSTGRES)
                .select(Tables.USERS.ID, Tables.USERS.EMAIL)
                .from(Tables.USERS)
                .where(Tables.USERS.ACTIVE.isTrue())
                .getSQL();

        log.info("Generated query: {}", sql);
    }
}
