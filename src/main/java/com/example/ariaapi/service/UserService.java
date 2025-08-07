package com.example.ariaapi.service;

import com.example.ariaapi.entity.User;
import com.example.ariaapi.dto.request.UserCreateDto;
import com.example.ariaapi.dto.response.UserDecryptedDto;
import com.example.ariaapi.dto.response.UserSearchResponseDto;
import com.example.ariaapi.exception.DecryptionException;
import com.example.ariaapi.repository.UserRepository;
import com.example.ariaapi.util.HashingUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AriaCryptoService ariaCryptoService;
    private final DecryptionAuditService decryptionAuditService;

    @Transactional
    public User saveEncryptedUser(UserCreateDto dto) {
        try {
            User user = new User();
            // 각 필드가 null이 아니고 비어있지 않은 경우에만 암호화 로직 수행
            if (dto.getUsrNm() != null && !dto.getUsrNm().isEmpty()) {
                user.setUsrNm(ariaCryptoService.encrypt(dto.getUsrNm()));
                user.setUsrNmHash(HashingUtil.sha256(dto.getUsrNm()));
            }
            if (dto.getUsrTel() != null && !dto.getUsrTel().isEmpty()) {
                user.setUsrTel(ariaCryptoService.encrypt(dto.getUsrTel()));
                user.setUsrTelHash(HashingUtil.sha256(dto.getUsrTel()));
            }
            if (dto.getUsrBirth() != null && !dto.getUsrBirth().isEmpty()) {
                user.setUsrBirth(ariaCryptoService.encrypt(dto.getUsrBirth()));
                user.setUsrBirthHash(HashingUtil.sha256(dto.getUsrBirth()));
            }

            return userRepository.save(user);
        } catch (Exception e) {
            System.err.println("사용자 데이터 암호화 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("사용자 데이터를 저장할 수 없습니다.", e);
        }
    }

    @Transactional
    public List<User> saveAllEncryptedUsers(List<UserCreateDto> dtoList) {
        List<User> users = dtoList.stream()
                .map(dto -> {
                    try {
                        User user = new User();
                        if (dto.getUsrNm() != null && !dto.getUsrNm().isEmpty()) {
                            user.setUsrNm(ariaCryptoService.encrypt(dto.getUsrNm()));
                            user.setUsrNmHash(HashingUtil.sha256(dto.getUsrNm()));
                        }
                        if (dto.getUsrTel() != null && !dto.getUsrTel().isEmpty()) {
                            user.setUsrTel(ariaCryptoService.encrypt(dto.getUsrTel()));
                            user.setUsrTelHash(HashingUtil.sha256(dto.getUsrTel()));
                        }
                        if (dto.getUsrBirth() != null && !dto.getUsrBirth().isEmpty()) {
                            user.setUsrBirth(ariaCryptoService.encrypt(dto.getUsrBirth()));
                            user.setUsrBirthHash(HashingUtil.sha256(dto.getUsrBirth()));
                        }
                        return user;
                    } catch (Exception e) {
                        System.err.println("개별 사용자 데이터 암호화 중 오류 발생: " + dto.getUsrNm() + " - " + e.getMessage());
                        throw new RuntimeException("일부 사용자 데이터를 저장할 수 없습니다.", e);
                    }
                })
                .collect(Collectors.toList());
        return userRepository.saveAll(users);
    }

    /**
     * 해시된 키워드를 기반으로 사용자를 검색하고, IP 로그를 남깁니다.
     *
     * @param type 검색 유형 (usrNm, usrTel, usrBirth).
     * @param keyword 검색할 값.
     * @param ipAddress 요청자의 IP 주소.
     * @param purpose 복호화의 목적.
     * @return 복호화된 사용자 데이터를 담은 DTO.
     * @throws IllegalArgumentException 검색 유형이 유효하지 않을 경우.
     * @throws EntityNotFoundException 해당 키워드로 사용자를 찾을 수 없을 경우.
     * @throws DecryptionException 복호화 중 오류 발생 시.
     */
    @Transactional(readOnly = true)
    public UserSearchResponseDto searchUserByHash(String type, String keyword, String ipAddress, String purpose) {
        Optional<User> userOptional;
        String hashedKeyword = HashingUtil.sha256(keyword);
        boolean success = false;

        try {
            switch (type) {
                case "usrNm":
                    userOptional = userRepository.findByUsrNmHash(hashedKeyword);
                    break;
                case "usrTel":
                    userOptional = userRepository.findByUsrTelHash(hashedKeyword);
                    break;
                case "usrBirth":
                    userOptional = userRepository.findByUsrBirthHash(hashedKeyword);
                    break;
                default:
                    throw new IllegalArgumentException("유효하지 않은 검색 유형입니다: " + type);
            }

            User user = userOptional.orElseThrow(() -> new EntityNotFoundException("해당 키워드로 사용자를 찾을 수 없습니다."));

            try {
                UserSearchResponseDto result = new UserSearchResponseDto(
                        user.getId(),
                        ariaCryptoService.decrypt(user.getUsrNm()),
                        ariaCryptoService.decrypt(user.getUsrTel()),
                        ariaCryptoService.decrypt(user.getUsrBirth())
                );
                success = true;
                return result;
            } catch (Exception e) {
                System.err.println("사용자 데이터 복호화 중 오류 발생 (ID: " + user.getId() + "): " + e.getMessage());
                throw new DecryptionException("사용자 데이터 복호화에 실패했습니다.", e);
            }
        } finally {
            decryptionAuditService.logDecryptionAttempt(
                    "SYSTEM",
                    ipAddress,
                    purpose != null && !purpose.isEmpty() ? purpose : "목적 지정 안됨",
                    success,
                    "검색: " + type + "=" + keyword
            );
        }
    }

    @Transactional(readOnly = true)
    public UserDecryptedDto getDecryptedUserById(Long id, String ipAddress, String purpose) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID " + id + "에 해당하는 데이터를 찾을 수 없습니다."));

        boolean success = false;
        try {
            UserDecryptedDto dto = new UserDecryptedDto();
            dto.setId(user.getId());
            dto.setUsrNm(ariaCryptoService.decrypt(user.getUsrNm()));
            dto.setUsrTel(user.getUsrTel() != null ? ariaCryptoService.decrypt(user.getUsrTel()) : null);
            dto.setUsrBirth(user.getUsrBirth() != null ? ariaCryptoService.decrypt(user.getUsrBirth()) : null);
            success = true;
            return dto;
        } catch (Exception e) {
            System.err.println("사용자 데이터 복호화 중 오류 발생 (ID: " + user.getId() + "): " + e.getMessage());
            throw new DecryptionException("사용자 데이터 복호화에 실패했습니다.", e);
        } finally {
            decryptionAuditService.logDecryptionAttempt(
                    "SYSTEM",
                    ipAddress,
                    purpose != null && !purpose.isEmpty() ? purpose : "목적 지정 안됨",
                    success,
                    String.valueOf(id)
            );
        }
    }

    /**
     * 데이터베이스에 저장된 모든 사용자의 데이터를 조회하고 복호화합니다.
     * IP 주소와 목적을 받아 감사 로그를 기록합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserDecryptedDto> getAllDecryptedUsers(Pageable pageable, String ipAddress, String purpose) {
        boolean success = false;
        Page<UserDecryptedDto> decryptedUsers;
        try {
            Page<User> userPage = userRepository.findAll(pageable);
            decryptedUsers = userPage.map(user -> {
                UserDecryptedDto dto = new UserDecryptedDto();
                dto.setId(user.getId());
                try {
                    dto.setUsrNm(user.getUsrNm() != null ? ariaCryptoService.decrypt(user.getUsrNm()) : null);
                    dto.setUsrTel(user.getUsrTel() != null ? ariaCryptoService.decrypt(user.getUsrTel()) : null);
                    dto.setUsrBirth(user.getUsrBirth() != null ? ariaCryptoService.decrypt(user.getUsrBirth()) : null);
                } catch (Exception e) {
                    System.err.println("개별 사용자 복호화 실패 (ID: " + user.getId() + "): " + e.getMessage());
                    dto.setUsrNm("[복호화 실패]");
                    dto.setUsrTel("[복호화 실패]");
                    dto.setUsrBirth("[복호화 실패]");
                }
                return dto;
            });
            success = true;
            return decryptedUsers;
        } finally {
            decryptionAuditService.logDecryptionAttempt(
                    "SYSTEM",
                    ipAddress,
                    purpose != null && !purpose.isEmpty() ? purpose : "목적 지정 안됨",
                    success,
                    "모든 사용자"
            );
        }
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }
}
