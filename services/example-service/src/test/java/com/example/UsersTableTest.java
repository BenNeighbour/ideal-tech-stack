package com.example;

import com.example.db.Tables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsersTableTest {

    @Test
    void usersTableHasExpectedColumnCount() {
        assertEquals(4, Tables.USERS.fields().length);
    }
}
