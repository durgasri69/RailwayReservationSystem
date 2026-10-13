package com.railway.reservation.database;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

@Component
public class PassengerSchemaMigration implements ApplicationRunner {

    private static final Logger LOGGER =
            Logger.getLogger(PassengerSchemaMigration.class.getName());

    private final JdbcTemplate jdbcTemplate;

    public PassengerSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'passengers'
                  AND column_name = 'user_id'
                """,
                Integer.class
        );

        if (columnCount == null || columnCount == 0) {
            return;
        }

        List<String> foreignKeys = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND table_name = 'passengers'
                  AND column_name = 'user_id'
                  AND referenced_table_name IS NOT NULL
                """,
                String.class
        );

        for (String foreignKey : foreignKeys) {
            jdbcTemplate.execute(
                    "ALTER TABLE `passengers` DROP FOREIGN KEY `"
                            + foreignKey.replace("`", "``") + "`"
            );
        }

        jdbcTemplate.execute("ALTER TABLE `passengers` DROP COLUMN `user_id`");
        LOGGER.info("Removed legacy user_id column from passengers table.");
    }
}
