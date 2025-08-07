package com.example.ariaapi.service;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.ARIAEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

@Service
public class AriaCryptoService {

    // serverPortCheck는 디버깅용으로 추가된 것이므로, 필요 없으면 제거해도 됩니다.
    @Value("${server.port}")
    private String serverPortCheck;

    @Value("${app.keystore.location}")
    private String keyStoreLocation;

    @Value("${app.keystore.password}")
    private String keyStorePasswordString; // String으로 주입받음

    @Value("${app.keystore.key-alias}")
    private String keyAlias;

    @Value("${app.keystore.key-password}")
    private String keyPasswordString; // String으로 주입받음

    private final ResourceLoader resourceLoader;
    private SecretKey ariaSecretKey;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom;

    public AriaCryptoService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.secureRandom = new SecureRandom();

        // Bouncy Castle 프로바이더 등록 (애플리케이션 시작 시 한 번만 실행)
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("Bouncy Castle Security Provider registered.");
        }
    }

    /**
     * KeyStore에서 ARIA 비밀 키를 로드합니다.
     * 이 메소드는 Spring에 의해 모든 @Value 필드가 주입된 후 @PostConstruct에 의해 자동으로 호출됩니다.
     * 키 로드 실패 시 RuntimeException을 발생시켜 애플리케이션 초기화를 중단합니다.
     */
    @PostConstruct // <-- 이 어노테이션에 의해 자동으로 호출됩니다.
    private void loadAriaKeyFromKeyStore() {
        char[] keyStorePassword = null;
        char[] keyPassword = null;

        try {
            keyStorePassword = keyStorePasswordString.toCharArray();
            keyPassword = keyPasswordString.toCharArray();

            Resource resource = resourceLoader.getResource(keyStoreLocation);
            KeyStore ks = KeyStore.getInstance("PKCS12");

            try (InputStream is = resource.getInputStream()) {
                ks.load(is, keyStorePassword);
            }

            KeyStore.SecretKeyEntry skEntry = (KeyStore.SecretKeyEntry) ks.getEntry(keyAlias, new KeyStore.PasswordProtection(keyPassword));

            if (skEntry == null) {
                throw new IllegalStateException("ARIA Key not found in KeyStore with alias: " + keyAlias);
            }
            this.ariaSecretKey = skEntry.getSecretKey();

            if (!this.ariaSecretKey.getAlgorithm().equalsIgnoreCase("ARIA")) {
                throw new IllegalStateException("Loaded key is not an ARIA key: " + this.ariaSecretKey.getAlgorithm());
            }
            if (this.ariaSecretKey.getEncoded().length * 8 != 192) {
                System.err.println("Warning: ARIA key size is not 192 bits. Actual: " + (this.ariaSecretKey.getEncoded().length * 8) + " bits.");
            }

            System.out.println("ARIA Key loaded successfully from KeyStore. Server Port Check: " + serverPortCheck); // 디버깅용 출력
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ARIA key from KeyStore: " + e.getMessage(), e);
        } finally {
            // 보안을 위해 char[] 배열은 사용 후 명시적으로 지웁니다.
            if (keyStorePassword != null) Arrays.fill(keyStorePassword, '\0');
            if (keyPassword != null) Arrays.fill(keyPassword, '\0');
        }
    }

    /**
     * 평문을 ARIA GCM 방식으로 암호화하고 Base64로 인코딩하여 반환합니다.
     * 반환되는 문자열은 "Base64(IV):Base64(암호문+인증태그)" 형식입니다.
     * @param plainText 암호화할 원문
     * @return Base64 인코딩된 IV와 암호문+인증태그 문자열
     * @throws Exception 암호화 중 오류 발생 시
     */
    public String encrypt(String plainText) throws Exception {
        byte[] keyBytes = ariaSecretKey.getEncoded();
        byte[] iv = generateRandomIvBytes();
        byte[] messageBytes = plainText.getBytes(StandardCharsets.UTF_8);

        // AAD (Associated Data) - 필요에 따라 여기에 실제 데이터를 넣을 수 있습니다.
        // 예를 들어, 요청 ID, 사용자 ID 등 암호화되지 않지만 무결성 검증에 포함될 데이터
        // 여기서는 예시를 위해 빈 바이트 배열을 사용합니다.
        byte[] aad = new byte[0]; // 비어있는 AAD

        GCMModeCipher cipher = GCMBlockCipher.newInstance(new ARIAEngine());

        // 초기화 (암호화 모드, 키, IV, AAD)
        cipher.init(true, new AEADParameters(new KeyParameter(keyBytes), GCM_TAG_LENGTH_BITS, iv, aad));

        // 암호화된 데이터 + 인증 태그를 담을 배열 크기
        byte[] encryptedAndTaggedData = new byte[cipher.getOutputSize(messageBytes.length)];

        // 데이터 처리
        int processedBytes = cipher.processBytes(messageBytes, 0, messageBytes.length, encryptedAndTaggedData, 0);

        try {
            // 최종 블록 처리 및 인증 태그 생성
            processedBytes += cipher.doFinal(encryptedAndTaggedData, processedBytes);
        } catch (InvalidCipherTextException e) {
            throw new Exception("ARIA GCM authentication tag generation failed: " + e.getMessage(), e);
        }

        // 실제 암호화된 데이터 + 태그 길이만큼 잘라 반환
        byte[] finalEncryptedData = Arrays.copyOfRange(encryptedAndTaggedData, 0, processedBytes);

        // IV와 암호화된 데이터를 Base64로 인코딩하여 결합 후 반환
        return Base64.getEncoder().encodeToString(iv) + ":" +
                Base64.getEncoder().encodeToString(finalEncryptedData);
    }

    /**
     * "Base64(IV):Base64(암호문+인증태그)" 형식의 문자열을 ARIA GCM 방식으로 복호화하여 평문을 반환합니다.
     * @param encryptedCombinedData Base64 인코딩된 IV와 암호문+인증태그 문자열
     * @return 복호화된 평문 문자열
     * @throws Exception 복호화 중 오류 발생 시 (인증 실패 포함)
     */
    public String decrypt(String encryptedCombinedData) throws Exception {
        byte[] keyBytes = ariaSecretKey.getEncoded();

        // 저장된 데이터에서 IV와 암호문+태그 분리
        String[] parts = encryptedCombinedData.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encrypted data format. Expected IV:CiphertextWithTag.");
        }
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] cipherTextWithTag = Base64.getDecoder().decode(parts[1]);

        // AAD (Associated Data) - 암호화할 때와 동일한 AAD를 사용해야 합니다.
        byte[] aad = new byte[0]; // 암호화할 때와 동일한 빈 AAD 사용

        // GCMBlockCipher 객체 생성
        GCMModeCipher cipher = GCMBlockCipher.newInstance(new ARIAEngine());

        // 초기화 (복호화 모드, 키, IV, AAD)
        cipher.init(false, new AEADParameters(new KeyParameter(keyBytes), GCM_TAG_LENGTH_BITS, iv, aad));

        // 복호화된 데이터를 담을 배열 크기
        byte[] outputData = new byte[cipher.getOutputSize(cipherTextWithTag.length)];

        // 데이터 처리
        int processedBytes = cipher.processBytes(cipherTextWithTag, 0, cipherTextWithTag.length, outputData, 0);

        try {
            // 최종 블록 처리 및 인증 태그 검증
            processedBytes += cipher.doFinal(outputData, processedBytes);
        } catch (InvalidCipherTextException e) {
            // 이 예외는 암호문이 변조되었거나 키/IV/AAD가 일치하지 않아 인증 태그 검증에 실패했을 때 발생
            throw new Exception("ARIA GCM authentication tag verification failed (Ciphertext might be tampered or keys/IV/AAD incorrect).", e);
        }

        // 실제 복호화된 평문 길이만큼 잘라 반환
        return new String(Arrays.copyOfRange(outputData, 0, processedBytes), StandardCharsets.UTF_8); // UTF-8 경고 제거
    }

    /**
     * 안전한 무작위 IV(Initialization Vector) 바이트 배열을 생성합니다.
     * GCM_IV_LENGTH 만큼의 바이트 배열을 생성합니다.
     */
    private byte[] generateRandomIvBytes() {
        byte[] bytes = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}