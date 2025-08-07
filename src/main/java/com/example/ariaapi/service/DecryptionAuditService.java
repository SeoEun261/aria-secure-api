package com.example.ariaapi.service;

import com.example.ariaapi.entity.DecryptionAuditLog;
import com.example.ariaapi.repository.DecryptionAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class DecryptionAuditService {

    private final DecryptionAuditLogRepository auditLogRepository;

    @Autowired
    public DecryptionAuditService(DecryptionAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 복호화 시도에 대한 감사로그를 기록합니다.
     * @param userId 복호화를 시도한 사용자 식별자
     * @param ipAddress 요청자의 IP 주소
     * @param purpose 복호화 목적
     * @param success 복호화 성공 여부
     * @param targetDataId 복호화 대상 데이터의 식별자 (예: User ID)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDecryptionAttempt(String userId, String ipAddress, String purpose, boolean success, String targetDataId) {
        DecryptionAuditLog log = new DecryptionAuditLog();
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setPurpose(purpose);
        log.setSuccess(success);
        log.setTargetDataId(targetDataId);
        log.setDecryptionTime(LocalDateTime.now()); // 현재 시간 기록
        log.setCreatedAt(LocalDateTime.now()); // 생성 시간 기록

        try {
            auditLogRepository.save(log);
            System.out.println("감사로그 저장 성공: " + log.getLogId());
            System.out.flush();
        } catch (Exception e) {
            System.err.println("!!! [오류] 감사로그 저장 실패: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
        }
    }

    public Page<DecryptionAuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
