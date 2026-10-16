package com.example.demo.service;

import com.example.demo.model.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, Long userId, String details) {
        AuditLog log = new AuditLog(eventType, userId, details);
        auditLogRepository.save(log);
        System.out.println("AUDIT [" + eventType + "] User:" + userId + " -> " + details);
    }
}
