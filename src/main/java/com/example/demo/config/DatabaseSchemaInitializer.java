package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@Order(1)
public class DatabaseSchemaInitializer implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Ensure parent_comment_id column exists in post_comments table
            try {
                boolean columnExists = false;
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "post_comments", "parent_comment_id")) {
                    if (rs.next()) {
                        columnExists = true;
                    }
                }
                if (!columnExists) {
                    stmt.execute("ALTER TABLE post_comments ADD COLUMN parent_comment_id BIGINT NULL");
                    System.out.println("[DatabaseSchemaInitializer] Added parent_comment_id column to post_comments");
                }
            } catch (Exception e) {
                System.out.println("[DatabaseSchemaInitializer] parent_comment_id check: " + e.getMessage());
            }

            // 2. Ensure utf8mb4 collation on post_comments table and content column so emojis store seamlessly
            try {
                stmt.execute("ALTER TABLE post_comments CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                stmt.execute("ALTER TABLE post_comments MODIFY content VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL");
                System.out.println("[DatabaseSchemaInitializer] Converted post_comments to utf8mb4 for emoji support");
            } catch (Exception e) {
                System.out.println("[DatabaseSchemaInitializer] utf8mb4 collation update: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[DatabaseSchemaInitializer] Database schema check note: " + e.getMessage());
        }
    }
}
