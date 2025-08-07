package com.example.ariaapi.controller;

import com.example.ariaapi.dto.request.UserCreateDto;
import com.example.ariaapi.dto.response.UserDecryptedDto;
import com.example.ariaapi.dto.request.UserPageRequest;
import com.example.ariaapi.dto.request.AuditPageRequest;
import com.example.ariaapi.dto.response.PageResponse;

import com.example.ariaapi.entity.User;
import com.example.ariaapi.entity.DecryptionAuditLog;

import com.example.ariaapi.service.UserService;
import com.example.ariaapi.service.DecryptionAuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final UserService userService;
    private final DecryptionAuditService decryptionAuditService;

    @Autowired
    public CryptoController(UserService userService, DecryptionAuditService decryptionAuditService) { // 생성자 수정
        this.userService = userService;
        this.decryptionAuditService = decryptionAuditService;
    }

    // --- API Endpoints ---

    /**
     * 단일 데이터를 암호화하여 데이터베이스에 저장하는 API 엔드포인트
     * POST /api/crypto/user
     * Request Body: { "usrNm": "Name", "usrTel": "Phone", "usrBirth": "Birth" }
     * Response: 저장된 User 엔티티 (암호화된 필드 포함)
     */
    @PostMapping("/user") // 단일 사용자 저장이므로 /user로 변경
    public ResponseEntity<User> saveEncryptedUser(@Valid @RequestBody UserCreateDto request) { // @Valid 추가
        try {
            User savedUser = userService.saveEncryptedUser(request); // UserService의 saveEncryptedUser 호출
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            System.err.println("단일 데이터 저장 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // 오류 발생 시 적절한 ErrorResponse DTO 반환 권장
        }
    }

    /**
     * 여러 데이터를 암호화하여 데이터베이스에 일괄 저장하는 API 엔드포인트
     * POST /api/crypto/users
     * Request Body: [ { "usrNm": "Name1", ... }, { "usrNm": "Name2", ... } ]
     * Response: 저장된 User 엔티티 리스트
     */
    @PostMapping("/users") // 여러 사용자 저장이므로 /users
    public ResponseEntity<List<User>> saveAllEncryptedUsers(@Valid @RequestBody List<UserCreateDto> requests) { // @Valid 추가
        try {
            List<User> savedUsers = userService.saveAllEncryptedUsers(requests); // UserService의 saveAllEncryptedUsers 호출
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUsers);
        } catch (Exception e) {
            System.err.println("일괄 데이터 저장 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // 오류 발생 시 적절한 ErrorResponse DTO 반환 권장
        }
    }

    /**
     * 특정 ID의 암호화된 데이터를 조회하고 복호화하여 반환하는 API 엔드포인트
     * GET /api/crypto/users/{id}/decrypted
     * (복호화 목적은 쿼리 파라미터로 받음)
     * Response: { "id": 1, "usrNm": "DecryptedName", ... }
     */
    @GetMapping("/users/{id}/decrypted")
    public ResponseEntity<UserDecryptedDto> getDecryptedUserById(
            @PathVariable Long id,
            @RequestParam(value = "purpose", required = false) String purpose, // 목적을 쿼리 파라미터로 받음
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String currentPurpose = (purpose != null && !purpose.isEmpty()) ? purpose : "Purpose Not Specified";

        try {
            // UserService의 getDecryptedUserById 호출
            UserDecryptedDto decryptedUser = userService.getDecryptedUserById(id, ipAddress, currentPurpose);
            return ResponseEntity.ok(decryptedUser);
        } catch (IllegalArgumentException e) { // ID를 찾을 수 없을 때 (UserService에서 던지는 예외)
            System.err.println("ID " + id + "에 해당하는 데이터 복호화 실패 (찾을 수 없음): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) { // 기타 복호화 실패 (UserService에서 예외를 던지지 않으므로 발생하지 않음)
            System.err.println("ID " + id + "에 해당하는 데이터 복호화 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 데이터베이스에 저장된 모든 암호화된 데이터를 조회하고 복호화하여 반환하는 API 엔드포인트
     * GET /api/crypto/users/all-decrypted
     * (복호화 목적은 쿼리 파라미터로 받음)
     * Response: [ { "id": 1, "usrNm": "DecryptedName", ... }, ... ]
     */
    @GetMapping("/users/all-decrypted")
    public ResponseEntity<Page<UserDecryptedDto>> getAllDecryptedUsers(
            @Valid @ModelAttribute UserPageRequest pageRequest,
            @RequestParam(value = "purpose", required = false) String purpose,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String currentPurpose = (purpose != null && !purpose.isEmpty()) ? purpose : "Purpose Not Specified";

        Pageable pageable = pageRequest.makePageRequest();

        try {
            Page<UserDecryptedDto> decryptedUsers = userService.getAllDecryptedUsers(pageable, ipAddress, currentPurpose);
            return ResponseEntity.ok(decryptedUsers);
        } catch (Exception e) {
            System.err.println("모든 데이터 복호화 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 모든 감사로그를 조회하는 API 엔드포인트 (관리/테스트용)
     * GET /api/crypto/audit-logs
     * Response: 페이지네이션된 감사로그 리스트 (PageResponse<DecryptionAuditLog>)
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<DecryptionAuditLog>> getAllAuditLogs(
                                                                             @Valid @ModelAttribute AuditPageRequest pageRequest) {
        Pageable pageable = pageRequest.makePageRequest();
        Page<DecryptionAuditLog> auditLogsPage = decryptionAuditService.getAllAuditLogs(pageable);

        PageResponse<DecryptionAuditLog> response = new PageResponse<>(auditLogsPage);
        return ResponseEntity.ok(response);
    }
}

