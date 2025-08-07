# ARIA API 서비스

---

## 1. 프로젝트 소개

이 프로젝트는 ARIA 암호화 알고리즘을 활용하여 **개인정보를 데이터베이스에 암호화된 형태로 저장하고, 조회 요청 시 복호화하여 제공하는  RESTful API 서비스**입니다. 

주요 기능:
* 개인정보 암호화/복호화 API: usrNm, usrTel, usrBirth 등 민감 정보를 ARIA 256으로 암복호화하여 관리합니다.
* 감사 로그(Audit Log) 시스템: 복호화 요청 시 purpose를 기록하여 데이터 접근의 목적과 이력을 추적합니다.
* JDBC Batch 처리: 대량의 데이터를 효율적으로 저장하기 위해 배치 처리를 적용했습니다.
* 동적 KeyStore 관리: 애플리케이션 실행 전 KeyStoreChooser 도구를 통해 키스토어 파일 경로와 비밀번호를 유연하게 설정할 수 있도록 구현했습니다.
* 페이지네이션 (Pagination): 복호화된 개인정보를 페이지별로 탐색하는 기능
* 키워드 검색 (Keyword Search): 이름,생일,전화번호 등으로 개인정보 검색
* 성능 테스트: PerformanceTest를 통해 암호화/복호화 및 대량 데이터 처리 성능을 검증했습니다.


---

## 2. 기술 스택 (Tech Stack)

이 프로젝트는 다음과 같은 기술 스택으로 구성됩니다.

* **언어:** Java 17
* **프레임워크:** Spring Boot 3.5.3
* **데이터베이스:** PostgreSQL 15
* **빌드 도구:** Gradle
* **보안 라이브러리:** Bouncy Castle (ARIA 암호화/복호화 구현)
* **기타:** Lombok, Spring Data JPA
* **키:** Java KeyStore 
---

## 3. 개발 환경 설정

프로젝트를 로컬에서 실행하기 위한 설정 방법입니다.

### 3.1 필수 요구사항

* Java Development Kit (JDK) 17 이상
* Gradle (별도 설치 필요 없이 `./gradlew` 사용 가능)
* 선택 사항: IntelliJ IDEA, VS Code 등 Java 개발 IDE

### 3.2 키스토어 설정

이 프로젝트는 개인정보 암호화/복호화를 위해 **Java KeyStore (PKCS12 형식)**에 ARIA 비밀 키를 안전하게 저장하고 관리합니다. 키스토어 설정은 KeyStoreChooser라는 보조 도구를 통해 자동으로 처리하여 개발 편의성을 높였습니다.
1. 키스토어 파일 생성:
* 아직 키스토어 파일이 없다면, src/main/java/com/example/ariaapi/util/KeyStoreGenerator.java 파일을 실행하여 키스토어 파일을 생성합니다.
* 이 프로그램은 src/main/resources 폴더에 mykeystore.p12 파일을 생성합니다.
* 생성 시 입력한 비밀번호와 키 별칭은 다음 단계에서 사용되므로 기억해 두세요.

2. KeyStoreChooser 실행:
* src/main/java/com/example/ariaapi/util/KeyStoreChooser.java 파일의 main 메소드를 실행합니다.
* 프로그램이 실행되면 키스토어 파일을 선택하는 창이 나타납니다. mykeystore.p12 파일을 선택하세요.
* 이후 콘솔의 안내에 따라 키스토어 파일 비밀번호, 키 별칭, 키 비밀번호를 차례로 입력합니다.

3. 자동 업데이트 확인:
* 입력이 완료되면, KeyStoreChooser 도구가 src/main/resources/application.properties와 src/test/resources/application.properties 파일을 자동으로 업데이트합니다.
* 이제 두 파일에 KeyStore 관련 설정이 올바르게 기록되었으므로, 별도의 수동 수정 없이 메인 애플리케이션을 실행할 수 있습니다.

---
## 4. API 엔드포인트 및 사용법

제공되는 주요 API 엔드포인트와 요청/응답 예시입니다.

### 4.1 단일 개인정보 암호화하여 DB에 저장

* **설명:** 개인정보를 ARIA GCM 알고리즘으로 암호화하여 DB에 저장
* **URL:** `/api/crypto/user`
* **HTTP 메서드:** `POST`
* **요청 Body (JSON):**
    ```json
    {
    "usrNm": "김철수",
    "usrTel": "010-9876-5432",
    "usrBirth": "1995-07-23"
    }
    ```
* **응답 Body (JSON - 성공 시):**
    ```json
    {
    "id": 1,
    "usrNm": "YpA6OdKu0CsRmapz:aw+4DsH5FVTQm5P6Ur/u+WLVLjzNyDu8hQ==",
    "usrTel": "+7jKpHd2KhM5dVWm:MNj+lA9mAO+BLrwuIfG0j3HD1G8qMKhpwFAQWo0=",
    "usrBirth": "bQNsmx549IIBBbfS:VUvczG2yW+tLgdEAtqZJgHH0I6FkdjExAl0=",
    "usrNmHash": "8JwhKcK+H5/U+XVzLJ9/keyyH69xjNIDCRfvng4bVbA=",
    "usrTelHash": "L6TYMVedYezsM5XZVfhfCAn3uGAOCZxfPPogA1FKe1Y=",
    "usrBirthHash": "3xqxv4+I4+z6GnnNrS+fb4Is1JENO5Hvn3O2uKfjdGk="
  }
    ```

### 4.2 여러 개인정보 암호화하여 DB에 저장

* **설명:** 여러 개인정보를 ARIA GCM 알고리즘으로 암호화하여 DB에 저장
* **URL:** `/api/crypto/users`
* **HTTP 메서드:** `POST`
* **요청 Body (JSON):**
    ```json
    [   
    {
        "usrNm": "이영희",
        "usrTel": null,
        "usrBirth": null
    },
    {
        "usrNm": "최지훈",
        "usrTel": null,
        "usrBirth": "1992-03-15"
    }
    ]
    ```
* **응답 Body (JSON - 성공 시):**
    ```json
  [
    {
        "id": 13,
        "usrNm": "xgdV1kWTNS7RNAu0:HfixIWv+VrhEYijN9iyWIujDQh9zNCvhSw==",
        "usrTel": null,
        "usrBirth": null,
        "usrNmHash": "AKQEwTNyvhojyo5u7oIRYU6yPoQKb9921JmjrMcADwU=",
        "usrTelHash": null,
        "usrBirthHash": null
    },
    {
        "id": 14,
        "usrNm": "6Rh3GllW4LMPWY7t:arrIv2ArQd0tdc29UbdwDgJst993Ff8eEw==",
        "usrTel": null,
        "usrBirth": "2+TWdBsr8AANP3Ee:1RDpisKLA9u+ePrVhNZ2P4+5sAeBnMeVRz4=",
        "usrNmHash": "q3pZmTgK6GSqMxnZmNY7cq4WpZ6H4j1rNBzKy7V1g4s=",
        "usrTelHash": null,
        "usrBirthHash": "dvb41s5OKhhgJ90JYB7wYM0lier4T49WWp41FVuhXos="
    }
  ]  
    ```

### 4.3 특정 개인정보 조회

* **설명:** 특정 id의 개인정보를 ARIA GCM 알고리즘으로 복호화하여 반환합니다.
* **URL:** `/api/crypto/users/1/decrypted?purpose=화면조회`
* **HTTP 메서드:** `GET`
* **응답 Body (JSON - 성공 시):**
    ```json
    {
      "id": 1,
      "usrNm": "이영희",
      "usrTel": null,
      "usrBirth": null
    }
    ```

### 4.4 모든 개인정보 조회 

* **설명:** DB에 등록된 모든 개인정보를 조회합니다. 
* **URL:** `api/users?page=2&size=2&purpose=화면조회`
* **HTTP 메서드:** `GET`
* **응답 Body (JSON - 성공 시):**
    ```json
    "results": [
        {
            "id": 10,
            "usrNm": "이하윤",
            "usrTel": "010-8901-2345",
            "usrBirth": "1994-12-18"
        },
        {
            "id": 9,
            "usrNm": "박시우",
            "usrTel": "010-7890-1234",
            "usrBirth": "1988-02-25"
        }
    ],
    "pageInfo": {
        "page": 2,
        "size": 2,
        "totalPage": 6,
        "totalElements": 12,
        "numberOfElements": 2
    }
    ```

### 4.5 키워드 검색
* **설명:** 키워드로 정보를 검색합니다.
* **URL:** `/api/users/search?type=usrNm&keyword=김민준&purpose=화면조회`
* **HTTP 메서드:** `GET`
* **응답 Body (JSON - 성공 시):**
  ```json
  {
    "usrId": 3,
    "usrNm": "김민준",
    "usrTel": "010-1234-5670",
    "usrBirth": "1995-03-22"
  }
  ```

### 4.6 감사로그 조회

* **설명:** 감사로그를 조회합니다.
* **URL:** `/api/crypto/audit-logs?page=1&size=1` 
* **HTTP 메서드:** `GET`
* **응답 Body (JSON - 성공 시):**
    ```json
    "results": [
        {
            "logId": 8,
            "userId": "SYSTEM",
            "decryptionTime": "2025-08-06T14:24:42.611087",
            "ipAddress": "0:0:0:0:0:0:0:1",
            "purpose": "화면조회",
            "success": true,
            "targetDataId": "모든 사용자",
            "createdAt": "2025-08-06T14:24:42.611087"
        }
    ],
    "pageInfo": {
        "page": 1,
        "size": 1,
        "totalPage": 8,
        "totalElements": 8,
        "numberOfElements": 1
    }
    ```

---

## 5. 핵심 비즈니스 로직 및 데이터 흐름
이 섹션에서는 프로젝트의 핵심 동작 방식과 더불어, ARIA 암호화/복호화 로직이 시스템 내에서 어떻게 적용되는지에 대한 상세한 데이터 흐름을 설명합니다.

### 5.1. 데이터 저장 흐름 (Encryption Flow)
1. 데이터 입력: 클라이언트로부터 개인정보 이름(**usrNm**), 전화번호(**usrTel**), 생년월일(**usrBirth**)과 같은 민감한 원문(Plaintext) 데이터가 API를 통해 시스템으로 들어옵니다.

2. 암호화 서비스 호출: 각 민감 데이터 필드는 **AriaCryptoService**로 전달됩니다.

3. ARIA GCM 암호화: AriaCryptoService는 전달받은 원문 데이터를 ARIA GCM(Galois/Counter Mode) 방식으로 암호화합니다. GCM 모드는 데이터의 **기밀성(Confidentiality)**과 **무결성(Integrity)**을 동시에 보장합니다.

4. 암호문 저장 형식: 암호화된 각 필드 데이터는 다음과 같은 형태로 변환되어 데이터베이스에 저장됩니다.
* Base64(Initialization Vector, IV) : Base64(Ciphertext + Authentication Tag)
* 여기서 IV는 각 암호화 작업마다 고유하게 생성되어 재사용 공격을 방지합니다.
* Authentication Tag는 데이터의 위변조 여부를 검증하는 데 사용됩니다.

5. DB 저장: 최종적으로 암호화된 형태로 변환된 개인 정보 데이터는 데이터베이스(**User 테이블의 usrNm, usrTel, usrBirth 필드** 등)에 안전하게 저장됩니다.

---
### 5.2. 데이터 조회 및 복호화 흐름 (Decryption Flow)
암호화된 데이터는 조회 시 자동으로 복호화되어 클라이언트에 원문 형태로 제공됩니다.

1. 데이터 조회 요청: 클라이언트가 암호화된 개인 정보(예: usrNm)를 포함하는 데이터를 조회하도록 API를 호출합니다.

2. DB에서 데이터 로드: 데이터베이스로부터 암호화된 개인 정보를 가져옵니다.

3. 복호화 서비스 호출: 조회된 암호화된 데이터는 **AriaCryptoService**로 전달됩니다.

4. ARIA GCM 복호화: AriaCryptoService는 저장된 Base64(IV):Base64(Ciphertext+AuthTag) 형태의 암호문을 사용하여 ARIA GCM 방식으로 복호화를 시도합니다.
* 복호화 과정에서 IV를 사용하여 정확한 복호화를 수행하고, Authentication Tag를 통해 데이터의 무결성을 검증합니다.

5. 복호화 성공/실패 처리:
* 성공 시: 데이터는 원문 형태로 안전하게 복원되어 클라이언트에게 반환됩니다.
* 실패 시: 복호화 과정에서 오류(예: 데이터 손상, 키 불일치 등)가 발생하면, 해당 필드는 사용자에게 **"[복호화 실패]"**와 같이 표시되어 데이터 이상을 알립니다. (혹은 별도의 오류 응답 처리)

6. 감사 로깅: 복호화가 시도될 때마다 **DecryptionAuditService**를 통해 감사 로그가 기록됩니다. (자세한 내용은 5.3 감사 로깅 참조)

7. 클라이언트 반환: 복호화되거나 실패 처리된 데이터가 API 응답으로 클라이언트에 전달됩니다.

---

### 5.3. 감사 로깅 (Audit Logging)
모든 민감 데이터 복호화 시도는 보안 감사를 위해 로그로 기록됩니다.

1. 복호화 시도 감지: AriaCryptoService에서 복호화가 시도될 때, **DecryptionAuditService**가 호출됩니다.

2. 로그 정보 수집: DecryptionAuditService는 다음과 같은 정보를 수집하여 로그를 생성합니다.


* **사용자:** 복호화를 시도한 사용자 식별자
* **시각:** 복호화 시도가 발생한 정확한 시각
* **요청 IP 주소:** 복호화를 요청한 클라이언트의 IP 주소
* **목적:** 복호화가 어떤 용도로 시도되었는지 (예: "화면 조회", "보고서 생성" 등)
* **성공 여부:** 복호화가 성공했는지 또는 실패했는지
* **복호화 대상:** 어떤 데이터 또는 개인정보 ID에 대한 복호화 시도인지 (예: targetDataId)


3. 로그 저장: 수집된 감사 로그는 데이터베이스 테이블에 안전하게 저장되어, 향후 보안 감사나 문제 발생 시 추적 자료로 활용됩니다.