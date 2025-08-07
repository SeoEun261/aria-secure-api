package com.example.ariaapi.repository;

import com.example.ariaapi.entity.DecryptionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecryptionAuditLogRepository extends JpaRepository<DecryptionAuditLog, Long> {
}