package com.example.ariaapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "decryption_audit_log_table", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DecryptionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id", length = 100) // 복호화를 시도한 사용자 식별자
    private String userId; // 현재 로그인 시스템 없으므로, 조회된 User ID 또는 "SYSTEM" 등

    @Column(name = "decryption_time", nullable = false) // 복호화 수행 일시
    private LocalDateTime decryptionTime;

    @Column(name = "ip_address", length = 45) // 복호화 요청한 IP (IPv4/IPv6 지원)
    private String ipAddress;

    @Column(name = "purpose", length = 500) // 복호화 목적 (예: "화면조회", "보고용")
    private String purpose;

    @Column(nullable = false) // 복호화 성공 여부
    private Boolean success;

    @Column(name = "target_data_id", length = 100) // 어떤 데이터(레코드)를 복호화했는지 식별키 (복호화된 User의 ID)
    private String targetDataId;

    @Column(name = "created_at", nullable = false) // 로그 생성 시각 (JPA @PrePersist 또는 생성자에서 설정)
    private LocalDateTime createdAt;

    // 편의를 위한 생성자
    public DecryptionAuditLog(String userId, LocalDateTime decryptionTime, String ipAddress, String purpose, Boolean success, String targetDataId) {
        this.userId = userId;
        this.decryptionTime = decryptionTime;
        this.ipAddress = ipAddress;
        this.purpose = purpose;
        this.success = success;
        this.targetDataId = targetDataId;
        this.createdAt = LocalDateTime.now(); // 로그 생성 시각 자동 설정
    }
}