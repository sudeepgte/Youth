package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseRepairService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void repairDatabase() {
        System.out.println("[DB-REPAIR] Starting database orphaned record cleanup...");

        // 1. Clean up child tables first
        safeExecute("DELETE FROM message_read_receipt WHERE message_id NOT IN (SELECT id FROM chat_message) OR user_id NOT IN (SELECT id FROM users)");
        safeExecute("DELETE FROM message_reactions WHERE message_id NOT IN (SELECT id FROM chat_message) OR user_id NOT IN (SELECT id FROM users)");
        safeExecute("DELETE FROM conversation_participants WHERE conversation_id NOT IN (SELECT id FROM conversation) OR user_id NOT IN (SELECT id FROM users)");

        // 2. Clean up major entities
        safeExecute("DELETE FROM chat_message WHERE sender_id NOT IN (SELECT id FROM users) AND sender_id IS NOT NULL");
        safeExecute("DELETE FROM conversation WHERE creator_id NOT IN (SELECT id FROM users) AND creator_id IS NOT NULL");

        // 3. Clean up events
        safeExecute("DELETE FROM event_registrations WHERE event_id NOT IN (SELECT id FROM events) OR user_id NOT IN (SELECT id FROM users)");
        safeExecute("DELETE FROM events WHERE creator_id NOT IN (SELECT id FROM users) AND creator_id IS NOT NULL");

        System.out.println("[DB-REPAIR] Database cleanup completed successfully.");
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Table might not exist yet or no orphaned records
        }
    }
}
