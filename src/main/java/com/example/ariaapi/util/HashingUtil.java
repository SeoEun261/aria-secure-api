package com.example.ariaapi.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * SHA-256 해싱 유틸리티 클래스.
 *
 * 이 클래스는 주어진 문자열을 SHA-256 해시로 변환하는 기능을 제공합니다.
 * 해싱된 값은 항상 동일한 입력에 대해 동일한 출력을 보장하므로,
 * 암호화된 데이터의 검색을 위한 인덱스 컬럼으로 사용됩니다.
 */
public class HashingUtil {

    private static final String HASHING_ALGORITHM = "SHA-256";

    /**
     * 입력된 문자열을 SHA-256 해시로 변환합니다.
     *
     * @param input 해시로 변환할 문자열 (예: 사용자 이름, 전화번호)
     * @return Base64로 인코딩된 SHA-256 해시 문자열
     */
    public static String sha256(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASHING_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}