package com.example.ariaapi.controller;

import com.example.ariaapi.dto.request.UserCreateDto;
import com.example.ariaapi.dto.response.UserDecryptedDto;
import com.example.ariaapi.dto.response.UserSearchResponseDto;
import com.example.ariaapi.dto.request.UserPageRequest;
import com.example.ariaapi.dto.response.PageResponse;

import com.example.ariaapi.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserCreateDto dto) {
        try {
            userService.saveEncryptedUser(dto);
            return new ResponseEntity<>(Collections.singletonMap("message", "사용자 정보가 성공적으로 저장되었습니다."), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Collections.singletonMap("error", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam("type") String type,
            @RequestParam("keyword") String keyword,
            @RequestParam("purpose") String purpose,
            HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            UserSearchResponseDto user = userService.searchUserByHash(type, keyword, ipAddress, purpose);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException ex) {
            return new ResponseEntity<>(Collections.singletonMap("error", ex.getMessage()), HttpStatus.NOT_FOUND);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(Collections.singletonMap("error", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestParam("purpose") String purpose,
            HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            UserDecryptedDto user = userService.getDecryptedUserById(id, ipAddress, purpose);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(Collections.singletonMap("error", ex.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(Collections.singletonMap("error", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDecryptedDto>> getAllUsers(
                                                                       @Valid @ModelAttribute UserPageRequest pageRequest,
                                                                       @RequestParam("purpose") String purpose,
                                                                       HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();

        Pageable pageable = pageRequest.makePageRequest();

        Page<UserDecryptedDto> usersPage = userService.getAllDecryptedUsers(pageable, ipAddress, purpose);

        PageResponse<UserDecryptedDto> response = new PageResponse<>(usersPage);

        return ResponseEntity.ok(response);
    }
}