package com.example.ariaapi;

import com.example.ariaapi.dto.request.UserCreateDto;
import com.example.ariaapi.dto.request.UserPageRequest;
import com.example.ariaapi.dto.response.UserDecryptedDto;
import com.example.ariaapi.entity.User;
import com.example.ariaapi.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

    @Autowired
    private UserService userService;

    private static final int NUM_RECORDS = 2000;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전 필요한 초기화 작업 (예: 기존 데이터 삭제)
        // userService.clearAllUsers(); // 필요하다면 주석 해제하여 테스트 전에 DB를 비웁니다.
    }

    @Test
    void testOverallPerformance() {
        log.info("=== [시작] 성능 테스트 - 데이터: {} ===", NUM_RECORDS);

        // 1. 암호화 및 일괄 저장
        log.info("--- [단계 1] 암호화 및 일괄 저장 시작 ---");
        long encryptionStartTime = System.nanoTime();

        List<UserCreateDto> usersToCreate = new ArrayList<>();
        IntStream.range(0, NUM_RECORDS).forEach(i -> {
            UserCreateDto dto = new UserCreateDto();
            dto.setUsrNm("테스트이름" + i);
            dto.setUsrTel("010-1234-" + String.format("%04d", i));
            dto.setUsrBirth(LocalDate.of(1990 + (i % 30), (i % 12) + 1, (i % 28) + 1).toString());
            usersToCreate.add(dto);
        });

        List<User> savedUsers = new ArrayList<>();
        try {
            savedUsers = userService.saveAllEncryptedUsers(usersToCreate);
        } catch (Exception e) {
            log.error("!!! [오류] 일괄 저장 실패: {}", e.getMessage(), e);
        }

        long encryptionEndTime = System.nanoTime();
        long encryptionDurationMillis = (encryptionEndTime - encryptionStartTime) / 1_000_000;
        log.info("--- [단계 1] 암호화 및 일괄 저장 완료 ---");
        log.debug("DEBUG: savedUsers.size() = {}", savedUsers.size());
        log.info("총 암호화 및 저장된 사용자 수: {} ({} ms 소요)", savedUsers.size(), encryptionDurationMillis);

        assertNotNull(savedUsers, "저장된 사용자 리스트는 null이 아니어야 합니다.");
        assertEquals(NUM_RECORDS, savedUsers.size(), "저장된 사용자 수가 예상과 다릅니다.");
        if (!savedUsers.isEmpty()) {
            log.debug("DEBUG: savedUsers.get(0).getId() = {}", savedUsers.get(0).getId());
        }


        // 2. 복호화 및 전체 조회 (페이지네이션 적용)
        log.info("\n--- [단계 2] 복호화 및 전체 조회 시작 ---");
        long decryptionStartTime = System.nanoTime(); // 변수 사용 시작

        String purposeForAllUsers = "성능 테스트 전체 조회";
        String testIpAddress = "127.0.0.1"; // 테스트용 IP 주소

        // 모든 레코드를 한 페이지에 가져오도록 Pageable 설정
        UserPageRequest pageRequest = new UserPageRequest();
        pageRequest.setPage(1); // 페이지는 1부터 시작
        pageRequest.setSize(NUM_RECORDS); // NUM_RECORDS 만큼의 모든 레코드를 가져오도록 사이즈 설정
        pageRequest.setSort("id,asc"); // 정렬 기준 설정 (DTO의 makePageRequest에 필요)

        Pageable pageable = pageRequest.makePageRequest();

        List<UserDecryptedDto> decryptedUsers = new ArrayList<>();
        try {
            // userService.getAllDecryptedUsers는 Page 객체를 반환합니다.
            Page<UserDecryptedDto> decryptedUsersPage = userService.getAllDecryptedUsers(pageable, testIpAddress, purposeForAllUsers);
            decryptedUsers = decryptedUsersPage.getContent(); // Page 객체에서 실제 내용(List)을 가져옵니다.
        } catch (Exception e) {
            log.error("!!! [오류] 전체 복호화 실패: {}", e.getMessage(), e);
        }

        long decryptionEndTime = System.nanoTime();
        long decryptionDurationMillis = (decryptionEndTime - decryptionStartTime) / 1_000_000; // 변수 사용 끝

        log.info("--- [단계 2] 복호화 및 전체 조회 완료 ---");
        log.debug("DEBUG: decryptedUsers.size() = {}", decryptedUsers.size());
        log.info("총 복호화된 사용자 수: {} ({} ms 소요)", decryptedUsers.size(), decryptionDurationMillis);

        assertNotNull(decryptedUsers, "복호화된 사용자 리스트는 null이 아니어야 합니다.");
        assertEquals(NUM_RECORDS, decryptedUsers.size(), "복호화된 사용자 수가 예상과 다릅니다.");
        if (!decryptedUsers.isEmpty()) {
            log.info("복호화된 첫 번째 사용자 이름: {}", decryptedUsers.get(0).getUsrNm());
        }

        // 3. 특정 ID로 조회 및 복호화 테스트 (단일)
        log.info("\n--- [단계 3] 단일 ID 조회 및 복호화 시작 ---");
        Long targetId = null;
        if (!savedUsers.isEmpty()) {
            targetId = savedUsers.get(0).getId(); // 첫 번째 저장된 사용자의 ID 사용
        } else {
            log.error("!!! [오류] 저장된 사용자가 없어 단일 ID 조회 테스트를 건너뜁니다.");
            return;
        }

        assertNotNull(targetId, "단일 조회 테스트를 위한 대상 ID는 null이 아니어야 합니다.");
        log.debug("DEBUG: 단일 조회 대상 ID = {}", targetId);

        String ipAddress = "127.0.0.1";
        String purpose = "테스트용 단일 사용자 ID 조회";
        long singleQueryStartTime = System.nanoTime();
        UserDecryptedDto singleUserById = null;
        try {
            singleUserById = userService.getDecryptedUserById(targetId, ipAddress, purpose);
        } catch (Exception e) {
            log.error("!!! [오류] 단일 ID 복호화 실패: {}", e.getMessage(), e);
        }
        long singleQueryEndTime = System.nanoTime();
        long singleQueryDurationMillis = (singleQueryEndTime - singleQueryStartTime) / 1_000_000;
        log.info("--- [단계 3] 단일 ID 조회 및 복호화 완료 ---");
        log.info("단일 사용자 (ID={}) 조회 및 복호화 시간: {} ms", targetId, singleQueryDurationMillis);

        log.debug("DEBUG: singleUserById (어설션 전) = {}", singleUserById);
        if (singleUserById != null) {
            log.debug("DEBUG: singleUserById.getId() = {}", singleUserById.getId());
            log.debug("DEBUG: singleUserById.getUsrNm() = {}", singleUserById.getUsrNm());
        }

        assertNotNull(singleUserById, "단일 조회된 사용자는 null이 아니어야 합니다.");
        assertEquals(targetId, singleUserById.getId(), "조회된 사용자 ID가 일치하지 않습니다.");
        log.info("조회된 사용자: ID={}, 이름={}, 전화번호={}", singleUserById.getId(), singleUserById.getUsrNm(), singleUserById.getUsrTel());
        log.info("=== [종료] 성능 테스트 ===");
    }
}