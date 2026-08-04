package com.ho.account.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class DatabaseTargetVerifier {

    void verify(MigrationConfiguration configuration) {
        try (Connection connection = DriverManager.getConnection(
                configuration.url(),
                configuration.user(),
                configuration.password());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
            if (!resultSet.next()
                    || !configuration.expectedDatabase().equals(resultSet.getString(1))) {
                throw new IllegalStateException(
                        "Connected database does not match the approved expected database");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Database target verification failed; inspect restricted DB logs",
                    exception);
        }
    }
}
