package com.craftaro.epicfarming.database.migrations;

import com.craftaro.core.database.DataMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ajoute la colonne de carburant (en secondes) aux fermes.
 */
public class _2_FuelMigration extends DataMigration {
    public _2_FuelMigration() {
        super(2);
    }

    @Override
    public void migrate(Connection connection, String tablePrefix) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tablePrefix + "active_farms ADD COLUMN fuel_seconds BIGINT NOT NULL DEFAULT 0");
        }
    }
}
