package tn.esprit.espritconnectbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseSchemaPatchConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner patchJobStatusEnum() {
        return args -> {
            try {
                String columnType = jdbcTemplate.queryForObject(
                        "SELECT COLUMN_TYPE " +
                                "FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_SCHEMA = DATABASE() " +
                                "AND TABLE_NAME = 'job_offer' " +
                                "AND COLUMN_NAME = 'status'",
                        String.class
                );

                if (columnType == null) {
                    return;
                }

                String normalized = columnType.toUpperCase();
                boolean hasPending = normalized.contains("'PENDING'");
                boolean hasRejected = normalized.contains("'REJECTED'");

                if (!hasPending || !hasRejected) {
                    jdbcTemplate.execute(
                            "ALTER TABLE job_offer " +
                                    "MODIFY COLUMN status " +
                                    "ENUM('PENDING','OPEN','CLOSED','REJECTED','DRAFT','EXPIRED') " +
                                    "NOT NULL"
                    );
                    log.info("Patched job_offer.status enum to include PENDING and REJECTED");
                }
            } catch (Exception ex) {
                log.warn("Unable to patch job_offer.status enum automatically: {}", ex.getMessage());
            }
        };
    }

    @Bean
    public CommandLineRunner patchJobApplicationCoverLetterColumn() {
        return args -> {
            try {
                String dataType = jdbcTemplate.queryForObject(
                        "SELECT DATA_TYPE " +
                                "FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_SCHEMA = DATABASE() " +
                                "AND TABLE_NAME = 'job_application' " +
                                "AND COLUMN_NAME = 'cover_letter_url'",
                        String.class
                );

                if (dataType == null) {
                    return;
                }

                String normalized = dataType.toUpperCase();
                if (!normalized.contains("TEXT")) {
                    jdbcTemplate.execute(
                            "ALTER TABLE job_application " +
                                    "MODIFY COLUMN cover_letter_url TEXT NULL"
                    );
                    log.info("Patched job_application.cover_letter_url column to TEXT");
                }
            } catch (Exception ex) {
                log.warn("Unable to patch job_application.cover_letter_url automatically: {}", ex.getMessage());
            }
        };
    }
}
