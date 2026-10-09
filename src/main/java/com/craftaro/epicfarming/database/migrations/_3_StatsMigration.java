package com.craftaro.epicfarming.database.migrations;

import com.craftaro.core.database.DataMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ajoute la production totale et le conteneur lié aux fermes.
 */
public class _3_StatsMigration extends DataMigration {
    public _3_StatsMigration() {
        super(3);
    }

    @Override
    public void migrate(Connection connection, String tablePrefix) throws SQLException {
        String[] columns = {
                "total_produced BIGINT NOT NULL DEFAULT 0",
                "linked_world VARCHAR(64) NOT NULL DEFAULT ''",
                "linked_x INTEGER NOT NULL DEFAULT 0",
                "linked_y INTEGER NOT NULL DEFAULT 0",
                "linked_z INTEGER NOT NULL DEFAULT 0"
        };
        for (String column : columns) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + tablePrefix + "active_farms ADD COLUMN " + column);
            }
        }
    }
}
