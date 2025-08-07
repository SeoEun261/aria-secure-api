package com.example.ariaapi.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.Security;
import java.util.Base64;

public class KeyStoreGenerator {

    public static void main(String[] args) throws Exception {

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            System.out.println("Bouncy Castle Security Provider registered successfully."); // 이 줄 추가
        } else {
            System.out.println("Bouncy Castle Security Provider already registered."); // 이 줄 추가
        }

        // 1. SecretKey 생성 (예: ARIA-192)
        KeyGenerator keyGen = KeyGenerator.getInstance("ARIA");
        keyGen.init(192); // 192비트 (24바이트) 또는 128 (16바이트), 256 (32바이트)
        SecretKey secretKey = keyGen.generateKey();

        System.out.println("Generated ARIA Key (Base64 for reference): " + Base64.getEncoder().encodeToString(secretKey.getEncoded()));

        // 2. KeyStore 객체 생성 (PKCS12 형식 권장)
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null); // 빈 KeyStore 초기화

        // 3. 비밀 키를 KeyStore에 저장할 정보 설정
        String alias = "myAriaKey"; // KeyStore 내에서 이 키를 식별할 이름 (별칭)
        char[] keyPassword = "mySecretKeyPassword".toCharArray(); // 이 키 자체에 대한 비밀번호 (KeyStore 비밀번호와 다를 수 있음)
        // **주의: 실제 운영에선 이 비밀번호도 안전하게 관리**
        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keyPassword);
        ks.setEntry(alias, skEntry, protParam);

        // 4. KeyStore 파일을 디스크에 저장
        char[] ksPassword = "myKeyStoreFilePassword".toCharArray(); // KeyStore 파일에 접근할 비밀번호
        // **주의: 이 비밀번호는 환경 변수 등으로 관리되어야 함!**
        String keyStoreFileName = "mykeystore.p12"; // 저장될 파일명
        String keyStoreFilePath = "src/main/resources/" + keyStoreFileName; // 프로젝트 리소스 폴더에 저장 (테스트용)

        try (FileOutputStream fos = new FileOutputStream(keyStoreFilePath)) {
            ks.store(fos, ksPassword);
        }

        System.out.println("\n-----------------------------------------------------");
        System.out.println("KeyStore '" + keyStoreFileName + "' created successfully!");
        System.out.println("Located at: " + keyStoreFilePath);
        System.out.println("KeyStore Password (storepass): " + new String(ksPassword));
        System.out.println("Key Alias: " + alias);
        System.out.println("Key Password (keypass): " + new String(keyPassword));
        System.out.println("-----------------------------------------------------");
    }
}